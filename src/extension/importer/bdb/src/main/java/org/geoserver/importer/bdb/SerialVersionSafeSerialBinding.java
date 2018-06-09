/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.bdb;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBase;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.util.FastOutputStream;
import java.io.*;

/** @author Ian Schneider <ischneider@opengeo.org> */
public class SerialVersionSafeSerialBinding<T> extends SerialBase implements EntryBinding<T> {

    @Override
    public T entryToObject(DatabaseEntry entry) {
        byte[] data = entry.getData();
        InputStream in = new ByteArrayInputStream(data, entry.getOffset(), entry.getSize());
        T info;
        try {
            info = (T) new SafeInputStream(in).readObject();
        } catch (Exception ioe) {
            throw new RuntimeException(ioe);
        }
        return info;
    }

    @Override
    public void objectToEntry(T e, DatabaseEntry entry) {
        FastOutputStream serialOutput = super.getSerialOutput(e);
        try {
            ObjectOutputStream out = new ObjectOutputStream(serialOutput);
            out.writeObject(e);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        final byte[] bytes = serialOutput.getBufferBytes();
        final int offset = 0;
        final int length = serialOutput.getBufferLength();
        entry.setData(bytes, offset, length);
    }

    static class SafeInputStream extends ObjectInputStream {

        public SafeInputStream(InputStream in) throws IOException {
            super(in);
        }

        protected ObjectStreamClass readClassDescriptor()
                throws IOException, ClassNotFoundException {
            ObjectStreamClass resultClassDescriptor =
                    super.readClassDescriptor(); // initially streams descriptor
            Class localClass =
                    Class.forName(
                            resultClassDescriptor
                                    .getName()); // the class in the local JVM that this descriptor
            // represents.
            if (localClass == null) {
                return resultClassDescriptor;
            }
            ObjectStreamClass localClassDescriptor = ObjectStreamClass.lookup(localClass);
            if (localClassDescriptor != null) { // only if class implements serializable
                final long localSUID = localClassDescriptor.getSerialVersionUID();
                final long streamSUID = resultClassDescriptor.getSerialVersionUID();
                if (streamSUID != localSUID) { // check for serialVersionUID mismatch.
                    final StringBuffer s =
                            new StringBuffer("Overriding serialized class version mismatch: ");
                    s.append("local serialVersionUID = ").append(localSUID);
                    s.append(" stream serialVersionUID = ").append(streamSUID);
                    Exception e = new InvalidClassException(s.toString());
                    e.printStackTrace();
                    resultClassDescriptor =
                            localClassDescriptor; // Use local class descriptor for deserialization
                }
            }
            return resultClassDescriptor;
        }
    }
}
