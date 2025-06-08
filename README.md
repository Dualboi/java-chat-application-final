# java-chat-application
Client and admin side discord like chat application running as a java server.

Running instructions
*Building the application*
    open up a command prompt cd into the directory for the application root, then type the command run.bat if in the IDE terminal use .\/run.bat this will start the server in the command line and prompt for a port number and password.

Terminal instructions

*Server:*
    java -jar target/java_chat_app-1.0-SNAPSHOT-jar-with-dependencies.jar server

*Client:*
    java -jar target/java_chat_app-1.0-SNAPSHOT-jar-with-dependencies.jar client

To run the client in javafx GUI
    java -jar target/java_chat_app-1.0-SNAPSHOT-jar-with-dependencies.jar GUI

To access the http server page:
Enter http://localhost:8080/ into your web browser once the server is running.

Other instructions
*Client:*
    you can enter the command "quit" at any time to quit the client

*/help for command information*