import { NodeIO } from '@gltf-transform/core';
import { ALL_EXTENSIONS } from '@gltf-transform/extensions';
import { MeshoptDecoder } from 'meshoptimizer';
await MeshoptDecoder.ready;
const io = new NodeIO().registerExtensions(ALL_EXTENSIONS).registerDependencies({ 'meshopt.decoder': MeshoptDecoder });
for (const f of process.argv.slice(2)) {
  const doc = await io.read(f);
  console.log('==', f);
  for (const m of doc.getRoot().listMaterials()) {
    const c = m.getBaseColorFactor().map(x => x.toFixed(2)).join(',');
    console.log(m.getName(), '| base', c, '| metal', m.getMetallicFactor().toFixed(2), '| rough', m.getRoughnessFactor().toFixed(2), '| alpha', m.getAlphaMode(), '| tex', !!m.getBaseColorTexture());
  }
}
