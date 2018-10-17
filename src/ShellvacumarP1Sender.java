/************************************************************************
* Programmer: Sheeyam Shellvacumar
* 
* UHCL ID: 1630300
* 
* Filename: ShellvacumarP1Sender.java
*
* Purpose: 
* This is  TCP Client which interacts with a TCP Login Server to send username/password to the 
* login server for authentication. On success, the login server will return
*
* Input: Username / Password / Points
*
* Output: Points / Itemlist / ItemPurchase
* 
* Arguments: (2) IpAddress, Port
* 
* How to Run: ShellvacumarP1Sender '127.0.0.1' 10300
* 
***********************************************************************/

//Start of Program
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class ShellvacumarP1Sender {
	// Main Method
	public static void main(String[] args) throws Exception {
		// Check if arguments are added
		if (args.length < 2) {
			System.out.println("Syntax: ShellvacumarP1Sender <serverIP> <port>");
			return;
		}

		String senderServerIP = args[0];
		int senderport = Integer.parseInt(args[1]);

		String username; // username String
		String password; // password String
		String points; // points String
		String serverResponse; // Stores reply from server.

		// Input stream - from user keyboard.
		BufferedReader fromUser = new BufferedReader(new InputStreamReader(System.in));

		// Creates the Client socket and binds to the server
		Socket clientSocket = new Socket(senderServerIP, senderport);

		// Output stream to send the sentence through this.
		DataOutputStream toServer = new DataOutputStream(clientSocket.getOutputStream());

		// Input stream to read the data from server.
		BufferedReader fromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		System.out.println("****** Welcome to the Distributed Shopping System ******");

		do {
		// Get Username from the User
		System.out.print("Enter Username > ");
		username = fromUser.readLine().trim();

		// Get Password from the User
		System.out.print("Enter Password > ");
		password = fromUser.readLine().trim();

		// Send Hashed Password to Login Server (MD5)
		toServer.writeBytes(username + " " + ShellvacumarP1Util.generateMD5String(password) + '\n');

		// Get Server Response for the Authentication
		serverResponse = fromServer.readLine();
		System.out.println("Server >> " + serverResponse);
		
		} while (serverResponse.equals("Login is Failed, Try Again"));

		// Check of Login Failed or Success
		//if (serverResponse.equals("Login is Failed, Try Again")) {
		//	System.out.println("Login is Failed, Try Again");
		//} else {
			// Print Points from the Server Response
			System.out.print("Your Shopping Points > " + serverResponse + "\n");

			// Assign Server Response to a Variable
			points = serverResponse;

			// Ask user input for item list request
			System.out.print("Do you want to request items? (yes/no)");
			String yesno = fromUser.readLine().trim().toLowerCase();

			if (yesno.equals("yes")) {
				toServer.writeBytes(points + '\n');

				// Print Item List on Client Space
				serverResponse = fromServer.readLine();
				System.out.println("Item List >> " + serverResponse);

				// Get Item Name from the User
				System.out.print("Enter Item to Buy >");
				String itemName = fromUser.readLine();

				// Get Item Quantity from the User
				System.out.print("Enter Item Quantity >");
				String itemQty = fromUser.readLine();
				toServer.writeBytes(itemName + " " + itemQty + '\n');

				// Print Product and Purchase Info
				serverResponse = fromServer.readLine();
				System.out.println("| Product | Left Qty | Points | Purchased Qty | >> " + serverResponse);

				// Put Client for Sleep to avoid data mismatch
				Thread.sleep(5000);

				// TCP Client sends the close message to server.
				clientSocket.close();
			} else {
				// TCP Client sends the close message to server.
				clientSocket.close();
			}
		//}
	}
}
// End of Program