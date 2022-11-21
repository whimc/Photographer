# WHIMC-Photographer Client

**This can currently only be ran on Windows machines!**

The client will run `photographer.py`. This script is responsible for taking screenshots whenever the server requests.
It connects to the server running `WHIMC-Photographer` via a WebSocket to communicate.

The server requires an independently connected Minecraft account with OP and admin group permissions. You should have it running on the same machine as the client and maximized to full screen. Once running activate it with /photographer collect <UUID will likely autofill>

## Setup

```powershell
python -m venv venv
./venv/Scripts/activate
pip install -r requirements.txt
```

## Configure
```
python photographer.py --host <main server ip> --port 8235
```

## UIUC-Specific
If you are connecting remotely to a UIUC client machine you may need to first connect via VPN and then add the domain UOFI\netID
