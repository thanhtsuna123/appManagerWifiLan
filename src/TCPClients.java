    import java.io.*;
    import java.net.*;

    public class TCPClients {
        private Socket socketChat;
        private DataInputStream in;
        private DataOutputStream out;
        private ServerSocket serverNhanFile;
        private boolean connected = false;
        private final ClientListener listener;

        public interface ClientListener {
            void onMessage(String msg);
            void onUserList(String[] users);
            void onFileReceived(String tenFile);
            void onDisconnect(String lyDo);
        }

        public TCPClients(ClientListener listener) {
            this.listener = listener;
        }

        public void ketNoi(String ip, int port, String username) throws Exception {
            socketChat = new Socket(ip, port);
            in = new DataInputStream(socketChat.getInputStream());
            out = new DataOutputStream(socketChat.getOutputStream());
            connected = true;

            // Gui ten va he dieu hanh sang Server
            String os = System.getProperty("os.name");
            out.writeUTF("LOGIN " + username + "|" + os);
            out.flush();

            // Mo cong 5002 o may client de san sang nhan file
            new Thread(this::langNgheFile).start();

            // Luong doc tin nhan va lenh dieu khien tu Server
            new Thread(() -> {
                try {
                    while (connected) {
                        String packet = in.readUTF();

                        if ("PING".equalsIgnoreCase(packet)) {
                            out.writeUTF("PONG");
                            out.flush();
                        } else if (packet.startsWith("USERS ")) {
                            String[] ds = packet.substring(6).split(",");
                            listener.onUserList(ds);
                        } else if (packet.startsWith("DISCONNECT")) {
                            listener.onDisconnect(packet);
                            break;
                        } else {
                            listener.onMessage(packet);
                        }
                    }
                } catch (IOException e) {
                    if (connected) listener.onDisconnect("Mat ket noi voi Server.");
                } finally {
                    ngatKetNoi();
                }
            }).start();
        }

        private void langNgheFile() {
            try {
                serverNhanFile = new ServerSocket(5002);
                while (connected) {
                    Socket sock = serverNhanFile.accept();
                    DataInputStream fIn = new DataInputStream(sock.getInputStream());
                    String tenFile = fIn.readUTF();
                    long doDai = fIn.readLong();

                    File thuMuc = new File("Download");
                    if (!thuMuc.exists()) thuMuc.mkdir();
                    File f = new File(thuMuc, tenFile);

                    FileOutputStream fOut = new FileOutputStream(f);
                    byte[] buffer = new byte[4096];
                    int read;
                    long daNhan = 0;
                    while (daNhan < doDai && (read = fIn.read(buffer, 0, (int)Math.min(buffer.length, doDai - daNhan))) != -1) {
                        fOut.write(buffer, 0, read);
                        daNhan += read;
                    }
                    fOut.flush();
                    fOut.close();
                    sock.close();
                    listener.onFileReceived(tenFile);
                }
            } catch (IOException ignored) {}
        }

        // Gui file qua port 5001 cua Server
        public void guiFile(String serverIp, String nguoiGui, String nguoiNhan, File file) {
            new Thread(() -> {
                try {
                    Socket sock = new Socket(serverIp, TCPServer.FILE_PORT);
                    DataOutputStream fOut = new DataOutputStream(sock.getOutputStream());
                    FileInputStream fIn = new FileInputStream(file);

                    fOut.writeUTF(nguoiNhan);
                    fOut.writeUTF(nguoiGui);
                    fOut.writeUTF(file.getName());
                    fOut.writeLong(file.length());

                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = fIn.read(buffer)) != -1) {
                        fOut.write(buffer, 0, read);
                    }
                    fOut.flush();
                    fIn.close();
                    sock.close();
                    listener.onMessage("[FILE] Da gui file " + file.getName() + " den " + nguoiNhan);
                } catch (Exception e) {
                    listener.onMessage("[LOI] Gui file that bai: " + e.getMessage());
                }
            }).start();
        }

        public void guiChatTatCa(String msg) {
            try {
                out.writeUTF("CHAT_ALL " + msg);
                out.flush();
            } catch (IOException ignored) {}
        }

        public void guiChatRieng(String nguoiNhan, String msg) {
            try {
                out.writeUTF("CHAT_ONE " + nguoiNhan + "|" + msg);
                out.flush();
            } catch (IOException ignored) {}
        }

        public void ngatKetNoi() {
            connected = false;
            try { if (out != null) out.writeUTF("EXIT"); } catch (Exception ignored) {}
            try { if (socketChat != null) socketChat.close(); } catch (Exception ignored) {}
            try { if (serverNhanFile != null) serverNhanFile.close(); } catch (Exception ignored) {}
        }
    }