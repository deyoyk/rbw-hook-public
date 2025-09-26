# rbw_ws_server.py
import asyncio
import json
import logging
from typing import Dict, Any

import websockets
from websockets.exceptions import ConnectionClosedOK, ConnectionClosedError

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

HOST = "0.0.0.0"
PORT = 25513
PATH = "/rbw/websocket"

warp_msg: Dict[str, Any] = {
    "type": "WARP_PLAYERS",
    "game_id": "1232",
    "map": "random",
    "is_ranked": True,
    "team1": [
        {"ign": "GirlsWannaDance"}
    ],
    "team2": [
        {"ign": "Cudgie7"}
    ]
}

connected_clients = set()


async def handler(websocket: websockets.WebSocketServerProtocol, path: str):
    logging.info("Incoming connection, path=%s, peer=%s", path, websocket.remote_address)
    if path != PATH:
        logging.warning("Rejecting connection: invalid path %s", path)
        await websocket.close(code=1008, reason="Invalid path")
        return

    connected_clients.add(websocket)

    try:
        async for message in websocket:
            logging.info("Received from %s: %s", websocket.remote_address, message)
    except (ConnectionClosedOK, ConnectionClosedError):
        logging.info("Connection closed: %s", websocket.remote_address)
    finally:
        connected_clients.discard(websocket)


async def console_input():
    loop = asyncio.get_event_loop()
    while True:
        cmd = await loop.run_in_executor(None, input, "> ")

        if cmd.strip() == "1":
            if connected_clients:
                data = json.dumps(warp_msg)
                await asyncio.gather(*[ws.send(data) for ws in connected_clients])
                logging.info("Sent warp_msg to all clients")
            else:
                logging.info("No clients connected")

        elif cmd.strip() == "2":
            logging.info("Shutting down server...")
            for ws in list(connected_clients):
                await ws.close(reason="Server shutting down")
            loop.stop()
            break

        else:
            logging.info("Unknown command: %s (use 1=send warp, 2=exit)", cmd.strip())


async def main():
    logging.info("Starting WebSocket server on %s:%d%s", HOST, PORT, PATH)
    async with websockets.serve(handler, HOST, PORT, max_size=None, ping_interval=20, ping_timeout=20):
        await console_input()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        logging.info("Server stopped by user (KeyboardInterrupt)")
