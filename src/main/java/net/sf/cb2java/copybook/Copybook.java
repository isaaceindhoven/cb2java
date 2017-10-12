/**
 *    cb2java - Dynamic COBOL copybook parser for Java.
 *    Copyright (C) 2006 James Watson
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 1, or (at your option)
 *    any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package net.sf.cb2java.copybook;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import net.sf.cb2java.Settings;
import net.sf.cb2java.Values;
import net.sf.cb2java.data.GroupData;
import net.sf.cb2java.data.Record;
import net.sf.cb2java.types.Element;
import net.sf.cb2java.types.Group;
import net.sf.cb2java.types.SignPosition;

/**
 * Represents a copybook data definition in memory
 * 
 * <p>acts as a Group element but as a special parent
 * being the copybook itself
 * 
 * @author James Watson
 */
public class Copybook extends Group implements Settings
{
    private String encoding = Settings.DEFAULT.getEncoding();
    private boolean littleEndian = Settings.DEFAULT.getLittleEndian();
    private String floatConversion = Settings.DEFAULT.getFloatConversion();
    private SignPosition signPosition = Settings.DEFAULT.getSignPosition();
    
    private Map<String, Redefine> redefines = new HashMap<>();

    private final Values values;
    
    /**
     * constructor
     *
     * @param name the name of the copybook
     */
    Copybook(String name, Values values)
    {
        super(name, 0, 0);
        
        this.values = values;
        this.values.setEncoding(encoding);
    }
    
    public Values getValues()
    {
        return values;
    }
    
    /**
     * redefined an element.  This behavior is partial, untested and
     * not officially supported (yet)
     * 
     * @param redefinition the redefinition (FOO REDEFINES BAR)
     * @param element the (more specific) element that replaces BAR
     */
    public void redefine(Redefinition redefinition, Element element)
    {
        Redefine redefine = new Redefine(redefinition.getFoo(), redefinition.getBar(), element);
        redefines.put(redefinition.getFoo(), redefine);
    }
    
    /**
     * Gets an aliased element. This behavior is partial, untested and
     * not officially supported (yet)
     * 
     * @param name Name of the redefinition (the FOO in FOO redefines BAR).
     * @return The (typically more specific) element specifying another interpretation of the same data.
     */
    public Element getAliased(String name)
    {
        Redefine redefine = redefines.get(name);
        return redefine == null ? null : redefine.getElement();
    }

    /**
     * Gets the map of redefines
     * @return map of redefines
     */
    public Map<String, Redefine> getRedefines() {
        return redefines;
    }


    /**
     * creates a new empty application data instance
     * 
     * @return a new empty application data instance
     */
    public Record createNew()
    {
        return new Record((GroupData) super.create());
    }
    
    /**
     * creates a new application data element with the given data
     * 
     * @param data the data to create the instance for
     * @return a new application data element with the given data
     * @throws IOException because
     */
    public Record parseData(byte[] data) throws IOException
    {
        return new Record((GroupData) parse(data));
    }
    
    public List<Record> parseData(InputStream stream) throws IOException
    {
        ByteBuffer buffer = new ByteBuffer(stream);        
        List<Record> list = new ArrayList<Record>();
        
        while (buffer.hasNext()) {
            list.add(new Record((GroupData) parse(buffer.getNext())));
        }
        
        return list;
    }
    
    /**
     * Sets the encoding for the copybook instance, used for parsing
     * and writing of data
     * 
     * @param encoding the encoding for the system
     */
    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }
    
    /**
     * retrieves the current encoding for text
     * 
     * @return the encoding for text
     */
    public String getEncoding()
    {
        return encoding;
    }
    
    public void setLittleEndian(boolean littleEndian)
    {
        this.littleEndian = littleEndian;
    }
    
    public boolean getLittleEndian()
    {
        return littleEndian;
    }
    
    public void setFloatConversion(String className)
    {
        this.floatConversion = className;
    }
    
    public String getFloatConversion()
    {
        return floatConversion;
    }
    
    public void setSignPosition(SignPosition position)
    {
        this.signPosition = position;
    }
    
    public SignPosition getSignPosition()
    {
        return signPosition;
    }
    
	@Override
	public int getColumnStart() {
		return Settings.DEFAULT.getColumnStart();
	}

	@Override
	public int getColumnEnd() {
		return Settings.DEFAULT.getColumnEnd();
	}

	/**
     * a helper class for buffering the data as it is processed
     * 
     * @author James Watson
     */
    public class ByteBuffer
    {
        // TODO allow strings, length delimiting
        private int position = 0;
        private byte[] internal = new byte[1024];
        private int size = 0;
        private final InputStream stream;
        
        public ByteBuffer(InputStream stream) throws IOException
        {
            this.stream = stream;
        }
        
        private boolean getMore()
        {
            try {
                byte[] array = new byte[1024];
                int read = 0;
                
                while ((read = stream.read(array)) >= 0) {
                    add(array, read);
                    
                    if (read >= getLength()) return true;
                }
                
                return read >= getLength();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        public void add(byte[] bytes, int length)
        {
            int space = internal.length - size;
            
            if (space < bytes.length) {
                byte[] temp = new byte[Math.min(internal.length * 2, internal.length + bytes.length)];
                System.arraycopy(internal, 0, temp, 0, size);
                internal = temp;
            }
            
            System.arraycopy(bytes, size, internal, size, length);
            
            size += length;
        }
        
        public boolean hasNext()
        {
            return (position < size) || getMore();
        }
        
        private int nextEnd()
        {
            if (!hasNext()) return -1;
            
            int next = position + getLength();
            
            return next > size ? size : next;
        }
        
        public byte[] getNext()
        {
            byte[] bytes = new byte[size];
            
            int end = nextEnd();
            
//            System.out.println("next end: " + end);
//            System.out.println("position: " + position);
            
            System.arraycopy(internal, position, bytes, 0, end - position);
            position = end;
            
            return bytes;
        }
    }

}