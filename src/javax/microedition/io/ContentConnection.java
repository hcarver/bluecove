package javax.microedition.io;

public interface ContentConnection extends StreamConnection {
	String getEncoding();

	long getLength();

	String getType();
}