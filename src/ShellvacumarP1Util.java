/***********************************************************************************
* Programmer: Sheeyam Shellvacumar
* 
* UHCL ID: 1630300
* 
* Filename: ShellvacumarP1Util.java
*
* Purpose: This is the utility class which has all the utility methods referred all the 
* client and server classes
*
* How to Run: No need to run. Just compile only.
* 
*************************************************************************************/

import java.io.*;
import java.security.*;

public class ShellvacumarP1Util {

	// MD5 Hashing Function
	public static String generateMD5String(String text) throws NoSuchAlgorithmException {
		// Create MessageDigest instance for MD5
		MessageDigest md = MessageDigest.getInstance("MD5");
		// Add password bytes to digest
		md.update(text.getBytes());
		// Get the hash's bytes
		byte[] bytes = md.digest();
		// This bytes[] has bytes in decimal format;
		// Convert it to hexadecimal format
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		// Get complete hashed password in hex format
		String generatedText = sb.toString();
		return generatedText;
	}
	
	// Method to Read String from File
	public static String readStringFromFile(String txtFile) {
		StringBuffer stringBuffer = new StringBuffer();
		try {
			File file = new File(txtFile);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				stringBuffer.append(line);
				stringBuffer.append(" | ");
			}
			fileReader.close();	
		} catch (IOException e) {
			e.printStackTrace();
		}
		return stringBuffer.toString();
	}
}
