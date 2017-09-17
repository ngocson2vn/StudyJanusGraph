package com.sonand.cassandra_wr;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.sql.Timestamp;
import java.util.List;
import javax.xml.bind.DatatypeConverter;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ColumnPath;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.NotFoundException;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SliceRange;
import org.apache.cassandra.thrift.TimedOutException;
import org.apache.cassandra.thrift.UnavailableException;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

/**
 * Hello world!
 *
 */
public class App {
  private static final Logger LOG = Logger.getLogger(App.class);
  // set up some constants
  private static final String UTF8 = "UTF8";
  private static final String HOST = "localhost";
  private static final int PORT = 9160;
  private static final ConsistencyLevel CL = ConsistencyLevel.ONE;

  public void insertData() throws InvalidRequestException, TException, UnsupportedEncodingException,
      UnavailableException, TimedOutException {
    TTransport tr = new TSocket(HOST, PORT);

    // new default in 0.7 is framed transport
    TFramedTransport tf = new TFramedTransport(tr);
    TProtocol proto = new TBinaryProtocol(tf);
    Cassandra.Client client = new Cassandra.Client(proto);
    tf.open();

    client.set_keyspace("keyspace1");
    String cfName = "standard1";
    String keyStr = "1";
    ByteBuffer userIDKey = ByteBuffer.wrap(keyStr.getBytes()); // this is a row key
    Timestamp clock = new Timestamp(System.currentTimeMillis());

    // create a representation of the Name column
    ColumnParent cp = new ColumnParent(cfName);

    // insert the name column
    LOG.debug("Inserting row for key " + new String(userIDKey.array()));
    Column nameCol = new Column(ByteBuffer.wrap("name".getBytes(UTF8)));
    nameCol.setValue("George Clinton".getBytes());
    nameCol.setTimestamp(clock.getTime());
    client.insert(userIDKey, cp, nameCol, CL);

    // insert the Age column
    Column ageCol = new Column();
    ageCol.setName("age".getBytes(UTF8));
    ageCol.setValue("69".getBytes());
    ageCol.setTimestamp(clock.getTime());

    client.insert(userIDKey, cp, ageCol, CL);

    LOG.debug("Row insert done.");

    tf.close();
    tr.close();
  }

  public void readData() throws UnsupportedEncodingException, InvalidRequestException, TException, NotFoundException,
      UnavailableException, TimedOutException {
    TTransport tr = new TSocket(HOST, PORT);

    // new default in 0.7 is framed transport
    TFramedTransport tf = new TFramedTransport(tr);
    TProtocol proto = new TBinaryProtocol(tf);
    Cassandra.Client client = new Cassandra.Client(proto);
    tf.open();

    client.set_keyspace("keyspace1");
    String cfName = "standard1";
    String keyStr = "1";
    ByteBuffer userIDKey = ByteBuffer.wrap(keyStr.getBytes()); // this is a row key
    ColumnPath colPathName = new ColumnPath(cfName);
    colPathName.setColumn("name".getBytes(UTF8));

    // read just the Name column
    LOG.debug("Reading Name Column:");
    Column col = client.get(userIDKey, colPathName, CL).getColumn();
    LOG.debug("Column name: " + new String(col.getName(), UTF8));
    LOG.debug("Column value: " + new String(col.getValue(), UTF8));
    LOG.debug("Column timestamp: " + col.getTimestamp());

    // create a slice predicate representing the columns to read
    // start and finish are the range of columns--here, all
    SlicePredicate predicate = new SlicePredicate();
    SliceRange sliceRange = new SliceRange();
    sliceRange.setStart(new byte[0]);
    sliceRange.setFinish(new byte[0]);
    predicate.setSlice_range(sliceRange);

    LOG.debug("Complete Row:");
    // read all columns in the row
    ColumnParent parent = new ColumnParent(cfName);
    List<ColumnOrSuperColumn> results = client.get_slice(userIDKey, parent, predicate, CL);

    // loop over columns, outputting values
    for (ColumnOrSuperColumn result : results) {
      Column column = result.column;
      LOG.debug(new String(column.getName(), UTF8) + " : " + new String(column.getValue(), UTF8));
    }

    LOG.debug("All done.");
    tf.close();
    tr.close();
  }

  @SuppressWarnings("restriction")
  public void readGraphIndex() throws UnsupportedEncodingException, InvalidRequestException, TException,
      NotFoundException, UnavailableException, TimedOutException {
    TTransport tr = new TSocket(HOST, PORT);

    // new default in 0.7 is framed transport
    TFramedTransport tf = new TFramedTransport(tr);
    TProtocol proto = new TBinaryProtocol(tf);
    Cassandra.Client client = new Cassandra.Client(proto);
    tf.open();

    client.set_keyspace("janusgraph");
    String cfName = "graphindex";
    byte[] parseHexBinary = DatatypeConverter.parseHexBinary("11a581");
    ByteBuffer key = ByteBuffer.wrap(parseHexBinary);

    // create a slice predicate representing the columns to read
    // start and finish are the range of columns--here, all
    SlicePredicate predicate = new SlicePredicate();
    SliceRange sliceRange = new SliceRange();
    sliceRange.setStart(new byte[0]);
    sliceRange.setFinish(new byte[0]);
    predicate.setSlice_range(sliceRange);

    // read all columns in the row
    ColumnParent parent = new ColumnParent(cfName);
    List<ColumnOrSuperColumn> results = client.get_slice(key, parent, predicate, CL);

    // loop over columns, outputting values
    for (ColumnOrSuperColumn result : results) {
      Column column = result.column;
      LOG.debug(DatatypeConverter.printHexBinary(column.getName()));
      LOG.debug(DatatypeConverter.printHexBinary(column.getValue()));
      LOG.debug(column.getTimestamp());
      LOG.debug("======");
    }

    LOG.debug("All done.");
    tf.close();
    tr.close();
  }

  public void readEdgeStore() throws UnsupportedEncodingException, InvalidRequestException, TException,
      NotFoundException, UnavailableException, TimedOutException {
    TTransport tr = new TSocket(HOST, PORT);

    // new default in 0.7 is framed transport
    TFramedTransport tf = new TFramedTransport(tr);
    TProtocol proto = new TBinaryProtocol(tf);
    Cassandra.Client client = new Cassandra.Client(proto);
    tf.open();

    client.set_keyspace("janusgraph");
    String cfName = "edgestore";
    byte[] parseHexBinary = DatatypeConverter.parseHexBinary("0000000000000309");
    ByteBuffer key = ByteBuffer.wrap(parseHexBinary);

    // create a slice predicate representing the columns to read
    // start and finish are the range of columns--here, all
    SlicePredicate predicate = new SlicePredicate();
    SliceRange sliceRange = new SliceRange();
    sliceRange.setCount(2147483647);
    sliceRange.setStart(DatatypeConverter.parseHexBinary("10C2"));
    sliceRange.setFinish(DatatypeConverter.parseHexBinary("10C3"));
    predicate.setSlice_range(sliceRange);

    // read all columns in the row
    ColumnParent parent = new ColumnParent(cfName);
    List<ColumnOrSuperColumn> results = client.get_slice(key, parent, predicate, CL);

    // loop over columns, outputting values
    for (ColumnOrSuperColumn result : results) {
      Column column = result.column;
      LOG.debug(DatatypeConverter.printHexBinary(column.getName()));
      // LOG.debug(new String(column.getName()));
      LOG.debug(DatatypeConverter.printHexBinary(column.getValue()));
      // LOG.debug(new String(column.getValue()));
      LOG.debug(column.getTimestamp());
      LOG.debug("======");
    }

    LOG.debug("All done.");
    tf.close();
    tr.close();
  }

  public static void main(String[] args) throws UnsupportedEncodingException, InvalidRequestException,
      UnavailableException, TimedOutException, TException, NotFoundException {
    App app = new App();
    // app.insertData();
    // app.readData();
    // app.readGraphIndex();
    app.readEdgeStore();
  }
}
