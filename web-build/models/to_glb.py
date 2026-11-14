# Blender: open a watch model, face the screen to the front, export GLB.
# blender -b --python to_glb.py -- <src .blend|.fbx> <out.glb> <screen object name> [hide,names]
import bpy, sys, mathutils
argv = sys.argv[sys.argv.index("--") + 1:]
src, out, screen_name = argv[0], argv[1], argv[2]
hide = set(argv[3].split(",")) if len(argv) > 3 and argv[3] else set()

if src.lower().endswith(".fbx"):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.wm.fbx_import(filepath=src)
else:
    bpy.ops.wm.open_mainfile(filepath=src)

scene = bpy.context.scene
for o in list(bpy.data.objects):
    if o.type != 'MESH' or o.name in hide or o.hide_render:
        bpy.data.objects.remove(o, do_unlink=True)
meshes = [o for o in bpy.data.objects if o.type == 'MESH']
# bake world transforms into the meshes
for o in meshes:
    mw = o.matrix_world.copy()
    o.parent = None
    o.matrix_world = mw
    o.data = o.data.copy()
    o.data.transform(o.matrix_world)
    o.matrix_world = mathutils.Matrix.Identity(4)
for o in list(bpy.data.objects):
    if o.type != 'MESH':
        bpy.data.objects.remove(o, do_unlink=True)

scr = bpy.data.objects[screen_name]
# screen normal (area weighted) and center
n = mathutils.Vector((0, 0, 0)); c = mathutils.Vector((0, 0, 0)); area = 0
for p in scr.data.polygons:
    n += p.normal * p.area; c += p.center * p.area; area += p.area
n.normalize(); c /= area
# make sure the normal points away from the case (outwards)
allc = sum((v.co for o in meshes for v in o.data.vertices), mathutils.Vector()) / sum(len(o.data.vertices) for o in meshes)
if (c - allc).dot(n) < 0:
    n = -n
q = n.rotation_difference(mathutils.Vector((0, -1, 0)))
R = q.to_matrix().to_4x4() @ mathutils.Matrix.Translation(-c)
for o in meshes:
    o.data.transform(R)
# strap runs along Z (up). If the model is wider in X, turn it 90 degrees about Y.
xs = [v.co.x for o in meshes for v in o.data.vertices]; zs = [v.co.z for o in meshes for v in o.data.vertices]
if (max(xs) - min(xs)) > (max(zs) - min(zs)):
    Ry = mathutils.Matrix.Rotation(1.5707963, 4, 'Y')
    for o in meshes: o.data.transform(Ry)
scr.name = "SCREEN"
for o in meshes:
    o.data.update()
print("SCREEN normal was", tuple(round(x, 3) for x in n), "meshes", len(meshes))
bpy.ops.export_scene.gltf(filepath=out, export_format='GLB', export_yup=True, export_apply=True,
                          export_materials='EXPORT',
                          export_image_format='NONE')
