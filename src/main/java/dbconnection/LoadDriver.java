package dbconnection;

public class LoadDriver {
	public static void loadDbDriver() {
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			System.out.println("Driver Loaded Successfully...");
		} catch(Exception e) {
			System.out.println("Driver Loading Failed...");
			e.printStackTrace();
		}
	}
}
