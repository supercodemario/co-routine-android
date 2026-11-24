import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA_DIR = ROOT / "data"
DB_PATH = DATA_DIR / "flowlab.db"

# Local learning secret. Override with FLOW_SECRET in a real deployment.
SECRET = os.environ.get("FLOW_SECRET", "flowlab-dev-secret-change-me")
TOKEN_HOURS = 24 * 7

# A device is "online" if it posted within this window.
ONLINE_SECONDS = 45

# Server pushes a tick on the stream so a Flow collector has events
# even when the ESP32 is unplugged.
TICK_SECONDS = 5

DEMO_EMAIL = "demo@flow.app"
DEMO_PASSWORD = "demo1234"
DEMO_NAME = "Demo"
DEMO_DEVICE_NAME = "Desk ESP32"
DEMO_DEVICE_KEY = "esp32-demo-key"
