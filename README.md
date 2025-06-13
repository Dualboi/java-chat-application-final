# Java Chat Application

A Discord-like chat application with both client and admin interfaces, running on a Java server.

## Getting Started

### Build & Run

**First step start the server:**
- Double-click `run-server.bat` in File Explorer  
  **or**
- From a terminal:
  ```
  ./run-server.bat
  ```

  **Start the client:**
- Double-click `run-client.bat` in File Explorer  
  **or**
- From a terminal:
  ```
  ./run-client.bat
  ```

  **Start the GUI:**
- Double-click `run-gui.bat` in File Explorer  
  **or**
- From a terminal:
  ```
  ./run-gui.bat
  ```

**Manual commands:**

- **Server:**
  ```
  java -jar target/java_chat_app-1.0-SNAPSHOT-jar-with-dependencies.jar server
  ```

- **Client:**
  ```
  java -jar target/java_chat_app-1.0-SNAPSHOT-jar-with-dependencies.jar client
  ```

- **JavaFX GUI Client:**
  ```
  java -jar target/java_chat_app-1.0-SNAPSHOT-jar-with-dependencies.jar GUI
  ```

### Web Interface

Once the server is running, open your browser and go to:  
[http://localhost:8080/](http://localhost:8080/)

## Usage

- **Client:**  
  Type `quit` at any time to exit the client.

- **Commands:**  
  Use `/help` in the client for a list of available commands.

---

## To view Javadocs 

- **Go to**
  ```
  application\target\site\apidocs\index.html
  ```