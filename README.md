# Bulletin Board System

Distributed client–server bulletin board implemented using Java TCP sockets.

## Features
- Multi-threaded server
- Multiple concurrent clients
- Post, query, and pin notes
- Custom text-based protocol

## Structure
- **server/** — Java server (run `BBoard`)
- **client/** — GUI client (run `ClientMain`)

---

## How to test (server + client)

First clone the repository using https://github.com/Mime-0/bulletin-board-main.git
With the files now open follow the instructions below to run

### 1. Start the server (first terminal)

```bash
cd server
javac *.java
Java BBoard <port> <board_width> <board_height> <note_width> <note_height> <color1> ... <colorN>
```
for ex. java BBoard 4554 200 100 30 30 red white green yellow 

Leave this running. You should see: `Bulletin Board server listening on port 4554`.

### 2. Start the client (second terminal)

```bash
cd client
javac *.java
java ClientMain
```

A window opens with Host `127.0.0.1` and Port `4554` 

### 3. In the client GUI

- Click **Connect** — the client connects to the server (you should see "Connected to server" in the output area).
- Use **POST** — enter x, y, color (e.g. `red`,'blue','yellow' or 'white), and message; the note appears on the board.
- Use **PIN** / **UNPIN** — enter coordinates; they affect the local board.
- Use **GET** to search through the notes for either notes that are pinned, have a specific colour, contain a specific x y, or text
- **SHAKE** / **CLEAR** — clear unpinned notes or all notes locally.



### 4. Stop the server

In the server terminal, press **Ctrl+C** to stop the server and use the terminal again.
