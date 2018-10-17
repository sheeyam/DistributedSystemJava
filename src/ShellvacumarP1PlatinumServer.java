/************************************************************************
* Programmer: Sheeyam Shellvacumar
* 
* UHCL ID: 1630300
* 
* Filename: ShellvacumarP1PlatinumServer.java
*
* Purpose: 
* This is UDP Group Server which gets input from the TCP login Server after
* Successful Authentication to retrieve the item list and purchase items. 
* This will update the item list file after purchase.
*
* Input: Username / Points / Item Request
*
* Output: Item List / Item Purchase Status
* 
* Arguments: (1) Port
* 
* How to Run: ShellvacumarP1PlatinumServer 10300
* 
***********************************************************************/
import java.io.*;
import java.net.*;
import java.util.*;

class ShellvacumarP1PlatinumServer implements Runnable {
	private DatagramSocket socket;
	private String itemFile;
	
	String[] itemNames = new String[1024]; // itemNames array
	String[] itemQtys = new String[1024]; // itemQtys array
	String[] itemPoints = new String[1024]; // itemPoints array

	public ShellvacumarP1PlatinumServer(int port, String itemf) throws SocketException {
		socket = new DatagramSocket(port);
		itemFile = itemf;
	}

	@Override
	public void run() {
		System.out.println("Threaded Platinum Group Server is Running  ");
		try {
			// Call Datagram Service
			dataGramService();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {

		int port = Integer.parseInt(args[0]);
		String itemListFile = Constants.PLATINUM_ITEM_LIST_FILE;
		
		try {
			ShellvacumarP1PlatinumServer server = new ShellvacumarP1PlatinumServer(port, itemListFile);
			Thread serverThread = new Thread(server);
			serverThread.start();
		} catch (Exception ex) {
			System.out.println("Socket error: " + ex.getMessage());
		}
	}

	// Function to call the Datagram Service
	private void dataGramService() throws NumberFormatException, Exception {
		while (true) {
			byte[] buf = new byte[512];
			DatagramPacket request = new DatagramPacket(buf, buf.length);
			socket.receive(request);

			// Get Client Address and Port
			InetAddress clientAddress = request.getAddress();
			int clientPort = request.getPort();

			String receivedString = new String(request.getData(), 0, request.getLength());

			String itemString = null;
			if (receivedString.equals(Constants.NOTHING)) {
				System.out.println("Processing.....  Item List Request");
				String[] allItems = getItemList(itemFile, Constants.ALLITEMS, 999, 999);
				itemString = allItems[0];
			} else {
				System.out.println("Processing..... Purchase Item Request (" + receivedString + ")");
				String[] receivedStringSplitArray = receivedString.split("\\s+");
				String[] items = getItemList(itemFile, receivedStringSplitArray[0],
						Integer.parseInt(receivedStringSplitArray[1]), Integer.parseInt(receivedStringSplitArray[2]));
				if (items[0].equals(Constants.INSUFF)) {
					itemString = items[0];
				} else {
					itemString = items[0] + " " + items[1] + " " + items[2] + " " + receivedStringSplitArray[1];
				}
			}

			DatagramPacket response = new DatagramPacket(itemString.getBytes(), itemString.getBytes().length,
					clientAddress, clientPort);
			socket.send(response);
		}
	}

	// Function to getItemList from Text File
	private String[] getItemList(String itemFile, String purchaseItem, int purchaseQty, int points) throws Exception {
		BufferedReader br = null;
		String[] retunarr = new String[3];

		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(itemFile));

			int i = 0;
			while ((sCurrentLine = br.readLine()) != null) {
				String[] arr = sCurrentLine.split(" ");

				// Enter them into separate arrays
				itemNames[i] = arr[0];
				itemQtys[i] = arr[1];
				itemPoints[i] = arr[2];
				i++;
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

		if (purchaseItem.equals(Constants.ALLITEMS)) {
			retunarr[0] = ShellvacumarP1Util.readStringFromFile(itemFile);
			System.out.println("Contents of file:");
			System.out.println(retunarr[0]);
		} else {
			int userIndex = Arrays.asList(itemNames).indexOf(purchaseItem);
			
			if(points >= (purchaseQty * Integer.parseInt(itemPoints[userIndex]))) {
				//Sufficient Points
				// Modify Purchase logic will come here--- later
				if(purchaseQty <= Integer.parseInt(itemQtys[userIndex])) {
					int stockleft = Integer.parseInt(itemQtys[userIndex]) - purchaseQty;
					itemQtys[userIndex] = String.valueOf(stockleft);
					retunarr[0] = itemNames[userIndex];
					retunarr[1] = itemQtys[userIndex];
					retunarr[2] = itemPoints[userIndex];
					
					// Write Modified Data to File
					try (PrintStream out = new PrintStream(new FileOutputStream(itemFile))) {
						for (int i = 0; i < itemNames.length; i++) {
							if(itemNames[i] != null && itemQtys[i] !=null && itemPoints[i] != null) {
								out.println(itemNames[i] + " " + itemQtys[i] + " " + itemPoints[i]);
							} else {
								//Do Nothing
							}
						}
					}
					// Write Modified Data to File - END
					System.out.println("Platinum Group Product List File Updated...");
				} else if(purchaseQty > Integer.parseInt(itemQtys[userIndex])) {
					// No Stock Available
					retunarr[0] = itemNames[userIndex];
					retunarr[1] = itemQtys[userIndex];
					retunarr[2] = itemPoints[userIndex];
				}
			} else {
				//In-Sufficient Points
				retunarr[0] = Constants.INSUFF;
			}
		}
		return retunarr;
	}
}


