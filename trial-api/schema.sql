-- Free trial time per device. trial_id = sha256("cronorep-trial:" + ANDROID_ID), hex.
CREATE TABLE IF NOT EXISTS trial_usage (
  trial_id   text PRIMARY KEY CHECK (trial_id ~ '^[0-9a-f]{64}$'),
  platform   text NOT NULL CHECK (platform IN ('wear', 'phone')),
  used_ms    bigint NOT NULL CHECK (used_ms >= 0),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
