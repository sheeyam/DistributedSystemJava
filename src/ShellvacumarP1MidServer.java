/************************************************************************
* Programmer: Sheeyam Shellvacumar
* 
* UHCL ID: 1630300
* 
* Filename: ShellvacumarP1MidServer.java
*
* Purpose: 
* This is a TCP Login/Mid Server which interacts TCP Login Server and the UDP Group Server
* to send username/password to the login server for authentication. 
* On success, the login server will return
*
* Input: Username / Password / Points
*
* Output: Points / Itemlist / ItemPurchase
* 
* Arguments: (1) Port
* 
* How to Run: ShellvacumarP1MidServer 10300
* 
***********************************************************************/
import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

class ShellvacumarP1MidServer implements Runnable {
	Socket connectionSocket;
	DataOutputStream toClient;
	BufferedReader fromClient;

	// Collections
	String[] usernames = new String[Constants.NUM_OF_USERS + 1]; // Usernames array
	String[] passwords = new String[Constants.NUM_OF_USERS + 1]; // Passwords array
	String[] points = new String[Constants.NUM_OF_USERS + 1]; // Points array
	String[] groups = new String[Constants.NUM_OF_GROUPS + 1]; // Groups array
	String[] ips = new String[Constants.NUM_OF_GROUPS + 1]; // IP Address array
	String[] ports = new String[Constants.NUM_OF_GROUPS + 1]; // Ports array
	String clientUserPass; // Stores Username and Password sent by client.

	// Constructor
	public ShellvacumarP1MidServer(Socket s) {
		try {
			System.out.println("Client Got Connected  ");
			connectionSocket = s;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Run Method
	@Override
	public void run() {
		try {
			fromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			toClient = new DataOutputStream(connectionSocket.getOutputStream());
			String responseToClient;
			String[] uparr;
			do {
			clientUserPass = fromClient.readLine();
			System.out.println("Client sent: " + clientUserPass);
			
			// Split Username Password String into array
			uparr = clientUserPass.split(" ");
			System.out.println("Username: " + uparr[0]);
			System.out.println("Password: " + uparr[1]);

			 // Response sent to client.
			responseToClient = doUserAuthentication(Constants.USER_LIST_FILE, uparr[0], uparr[1]);

			System.out.println(responseToClient);
			toClient.writeBytes(responseToClient + '\n');
			toClient.flush();
			
			} while(responseToClient.equals("Login is Failed, Try Again"));

			// Get Points from Client
			String points = fromClient.readLine();
			int shoppingPoints = Integer.parseInt(points);

			// Add Group text details into an array
			String groupArr[] = getGroupServerDetails(Constants.GROUP_LIST_FILE, shoppingPoints);
			String groupIp = groupArr[0];
			int groupPort = Integer.parseInt(groupArr[1]);
			String groupName = groupArr[2];

			System.out.println("**** User Group Details****");
			System.out.println("groupIp: " + groupIp);
			System.out.println("groupPort: " + groupPort);
			System.out.println("groupName: " + groupName);
			
            String allListGroupName = Constants.NOTHING;
            String requestAllItems = datagramService(groupIp, groupPort, Constants.REQUESTLIST, allListGroupName);

			toClient.writeBytes(requestAllItems + "\n");
			toClient.flush();

			String itemQuantity = fromClient.readLine(); // pendrive 1
			String itemQuantityPoints = itemQuantity + " " + points; // pendrive 1 1200
			System.out.println("Client Purchase Order | Qty: " + itemQuantityPoints);

			String purchaseRequestStatus = datagramService(groupIp, groupPort, Constants.PURCHASE, itemQuantityPoints);
			System.out.println(purchaseRequestStatus);
			if (purchaseRequestStatus.equals(Constants.INSUFF)) {
				toClient.writeBytes(Constants.INSUFFICIENT_POINTS_TEXT + "\n");
				toClient.flush();
				System.out.println("End of" + groupName.toUpperCase() + "Server Execution");
			} else {
				toClient.writeBytes(purchaseRequestStatus + "\n");
				toClient.flush();

				String[] purchaseRequestStatusSplitArray = purchaseRequestStatus.split("\\s+");
				int deductPoints = Integer.parseInt(purchaseRequestStatusSplitArray[3])
						* Integer.parseInt(purchaseRequestStatusSplitArray[2]);
				updateUserPoints(uparr[0], deductPoints, Constants.USER_LIST_FILE);
				System.out.println("End of Silver Server Execution");
			}
		} catch (SocketTimeoutException ex) {
			System.out.println("Timeout error: " + ex.getMessage());
			ex.printStackTrace();
		} catch (IOException ex) {
			System.out.println("Client error: " + ex.getMessage());
			ex.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	// Method which call the datagram service in the group server and returns the item strings.
	private String datagramService(String hostname, int port, String mode, String itemQtyPoints) throws IOException {
		InetAddress address = InetAddress.getByName(hostname);
		DatagramSocket socket = new DatagramSocket();
		while (true) {
			if (mode.equals(Constants.REQUESTLIST)) {
				DatagramPacket request = new DatagramPacket(itemQtyPoints.getBytes(), itemQtyPoints.getBytes().length,
						address, port);
				socket.send(request);

				byte[] buffer = new byte[512];
				DatagramPacket response = new DatagramPacket(buffer, buffer.length);
				socket.receive(response);

				String item = new String(buffer, 0, response.getLength());
				System.out.println(item);
				System.out.println();

				return item;
			} else if (mode.equals("PURCHASE")) {
				DatagramPacket request = new DatagramPacket(itemQtyPoints.getBytes(), itemQtyPoints.getBytes().length,
						address, port);
				socket.send(request);

				byte[] buffer = new byte[512];
				DatagramPacket response = new DatagramPacket(buffer, buffer.length);
				socket.receive(response);

				String item = new String(buffer, 0, response.getLength());
				System.out.println(item);
				System.out.println();

				return item;
			}
			socket.close();
		}
	}

	void updateUserPoints(String username, int deductPoints, String txtFile) throws FileNotFoundException {

		int userIndex = Arrays.asList(usernames).indexOf(username);

		if (Integer.parseInt(points[userIndex]) - deductPoints >= 0) {
			points[userIndex] = String.valueOf(Integer.parseInt(points[userIndex]) - deductPoints);
		} else {
			points[userIndex] = String.valueOf(0);
		}

		try (PrintStream out = new PrintStream(new FileOutputStream(txtFile))) {
			for (int i = 0; i < usernames.length; i++) {
				if (usernames[i] != null && passwords[i] != null && points[i] != null) {
					out.println(usernames[i] + " " + passwords[i] + " " + points[i]);
				} else {
					// DO Nothing
				}
			}
		}
		System.out.println("User List File Updated...");
	}

	// Method to Read and Populate Text File into Arrays
	String[] getGroupServerDetails(String txtFile, int points) {
		BufferedReader br = null;
		String[] retunarr = new String[3];
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(txtFile));

			int i = 0;
			while ((sCurrentLine = br.readLine()) != null) {
				String[] arr = sCurrentLine.split(" ");

				// Enter them into separate arrays
				groups[i] = arr[0];
				ips[i] = arr[1];
				ports[i] = arr[2];
				i++;
			}

			if (points >= 0 && points <= 999) {
				retunarr[0] = ips[1];
				retunarr[1] = ports[1];
				retunarr[2] = Constants.SILVER;
			} else if (points >= 1000 && points <= 4999) {
				retunarr[0] = ips[2];
				retunarr[1] = ports[2];
				retunarr[2] = Constants.GOLD;
			} else if (points >= 5000) {
				retunarr[0] = ips[3];
				retunarr[1] = ports[3];
				retunarr[2] = Constants.PLATINUM;
			} else {
				// Do Nothing
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return retunarr;
	}

	// Function to Do User Authentication
	String doUserAuthentication(String UserListFile, String uname, String pass) throws NoSuchAlgorithmException {
		BufferedReader buffReader = null;
		String responseToClientString = null;
		try {
			String sCurrentLine;
			buffReader = new BufferedReader(new FileReader(UserListFile));

			int i = 0;
			while ((sCurrentLine = buffReader.readLine()) != null) {
				String[] arr = sCurrentLine.split(" ");

				// Now if you want to enter them into separate arrays
				usernames[i] = arr[0];
				passwords[i] = arr[1];
				points[i] = arr[2];
				i++;
			}

			// User Authentication Logics below
			int userIndex = Arrays.asList(usernames).indexOf(uname);
			if (uname.toString().trim().equals(usernames[userIndex].trim()) && pass.toString().trim()
					.equals(ShellvacumarP1Util.generateMD5String(passwords[userIndex].trim()))) {
				responseToClientString = points[userIndex];
			} else {
				responseToClientString = "Login is Failed, Try Again";
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (buffReader != null)
					buffReader.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return responseToClientString;
	}

	// Main Method
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		System.out.println("Threaded TCP Mid Server is Running  ");

		// Port Argument
		int port = Integer.parseInt(args[0]);

		ServerSocket mysocket = new ServerSocket(port);
		while (true) {
			Socket sock = mysocket.accept();
			ShellvacumarP1MidServer server = new ShellvacumarP1MidServer(sock);
			Thread serverThread = new Thread(server);
			serverThread.start();
		}
	}
}
