package com.finc.stg;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.TypeCodec;

public class Main {
	static String[] CONTACT_POINTS = { "127.0.0.1" };
	static int PORT = 9042;

	private Cluster cluster;
	private Session session;

	public static void main(String[] args) {
		Main program = new Main();
		program.connect(CONTACT_POINTS, PORT);
		program.querySchema();
		program.close();
	}

	public void connect(String[] contactPoints, int port) {
		cluster = Cluster.builder().addContactPoints(contactPoints).withPort(port).build();
		System.out.printf("Connected to cluster: %s%n", cluster.getMetadata().getClusterName());
		session = cluster.connect();
	}

	/**
	 * Queries and displays data.
	 */
	public void querySchema() {

		ResultSet results = session
				.execute("SELECT * FROM janusgraph.janusgraph_ids LIMIT 10");

		System.out.printf("%-30s\t%-20s\t%-20s%n", "key", "column1", "value");
		System.out.println("-------------------------------+-----------------------+--------------------");

		for (Row row : results) {
			ProtocolVersion proto = cluster.getConfiguration().getProtocolOptions().getProtocolVersion();
			String key = TypeCodec.varchar().deserialize(row.getBytes("key"), proto);
			String column1 = TypeCodec.varchar().deserialize(row.getBytes("column1"), proto);
			String value = TypeCodec.varchar().deserialize(row.getBytes("value"), proto);
			
			System.out.printf("%-30s\t%-20s\t%-20s%n", key, column1, value);
		}

	}

	/**
	 * Closes the session and the cluster.
	 */
	public void close() {
		session.close();
		cluster.close();
	}
}
