package storage;

import java.lang.Integer;
import java.lang.reflect.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Collection;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import storage.*;

public class MemoryManager implements Serializable {
	
	Class partypes[];
	private Class mmclass;
	private long _maxSize;
	private long _currSize;
	transient private ObjectOutputStream _oos = null; 
	transient private ByteArrayOutputStream _baos = null;
	private static final long serialVersionUID = 1L;
	
	/* Size argument measured in MBytes */
	public MemoryManager(long maxSize) {
		// Setting up reflexion 
	//	mmclass = this.getClass();
		this.partypes = new Class[2];
		this.partypes[0] = new Object().getClass();
		this._currSize = 0;
		// FIXME FIXME FIXME
		this._maxSize = (maxSize * 1024 * 1024);
//		this._maxSize = 256*1024;
//		this._maxSize = 12;
	}

	/* Primitive types */
	int getSize(int var) 	 { return 4; }
	int getSize(boolean var) { return 1; }
	int getSize(char var) 	 { return 2; }
	int getSize(byte var) 	 { return 1; }
	int getSize(short var) 	 { return 2; }
	int getSize(long var) 	 { return 8; }
	int getSize(float var) 	 { return 4; }
	int getSize(double var)  { return 8; }

	/* JAVA String and Primitive Wrappers */
	int getSize(String str) { 
		return str.getBytes().length;
	}

	private int ByteSize = 0;
	int getSize(Byte var) {
		if (ByteSize == 0) {
			ByteSize = genericGetSize(var);
		}
		return ByteSize;
	}
	
	private int ShortSize = 0;
	int getSize(Short var) {
		if (ShortSize == 0) {
			ShortSize = genericGetSize(var);
		}
		return ShortSize;
	}

	private int IntegerSize = 0;
	int getSize(Integer var) {
		if (IntegerSize == 0) {
			IntegerSize = genericGetSize(var);
		}
		return IntegerSize;
	}

	private int LongSize = 0;
	int getSize(Long var) {
		if (LongSize == 0) {
			LongSize = genericGetSize(var);
		}
		return LongSize;
	}

	private int FloatSize = 0;
	int getSize(Float var) {
		if (FloatSize == 0) {
			FloatSize = genericGetSize(var);
		}
		return FloatSize;
	}

	private int DoubleSize = 0;
	int getSize(Double var) {
		if (DoubleSize == 0) {
			DoubleSize = genericGetSize(var);
		}
		return DoubleSize;
	}

	private int CharacterSize = 0;
	int getSize(Character var) {
		if (CharacterSize == 0) {
			CharacterSize = genericGetSize(var);
		}
		return CharacterSize;
	}

	private int BooleanSize = 0;
	int getSize(Boolean var) {
		if (ByteSize == 0) {
			ByteSize = genericGetSize(var);
		}
		return ByteSize;
	}

	// FIXME FIXME FIXME: make this smarter: count of elems in list * size of elem[0]
	/*int getSize(ArrayList list) {
		int totalSize = 0;
		for (Iterator<?> it = list.iterator(); it.hasNext(); ) {
			totalSize += getSize(it.next());
		}
		return totalSize;
	}*/

	public int getSize(Object obj) {	
		/* It may happen -- for special tricks (see DistinctOperator)
		 * that we receive the null object. In this case return 0 since
		 * no additional memory size is required. */
		if (obj == null) 
			return 0;	
		try {
			partypes[1] = obj.getClass();	
			//System.out.println("-------" + obj.getClass().getName());
			if (partypes[0].equals(partypes[1])) {
				System.out.println("OBJECT RECURSION BUG DETECTED!");
				return genericGetSize(obj);
			}
			Method m = this.getClass().getDeclaredMethod("getSize", partypes);
			return ((Integer)m.invoke(this, obj)).intValue();
		} catch (java.lang.NoSuchMethodException cnfe) { return genericGetSize(obj);
		} catch (java.lang.IllegalAccessException cnfe2) { System.out.println("EXCEPTIOOOOOON 4"); 
		} catch (java.lang.reflect.InvocationTargetException cnfe3) {  System.out.println(cnfe3.getMessage()); }
		return 0;
	}

	private void initMemoryStreams() {
		this._baos = new ByteArrayOutputStream();
		try {
			this._oos = new ObjectOutputStream(this._baos);
		} catch (IOException ioe) { System.out.println("EXCEPTIOOOOOON 2"); }
	}
	
	private int genericGetSize(Object obj) {
		if (this._baos == null || this._oos == null)
			initMemoryStreams();
		try {
			this._oos.reset();
			this._baos.reset();
			this._oos.writeObject(obj);
			this._oos.close(); 
			this._baos.close();
			return _baos.toByteArray().length;
		} catch (IOException ioe) { System.out.println("THN KATSAME!"); }
		return -1;
	}

	/* Checks if the store has enough bytes left to store bytesRequested
	 * size of objects */
	boolean hasExceededMaxSpace() {
//		System.out.println(this._currSize + " " + this._maxSize);
		return (this._currSize > this._maxSize);
	}
	
	void allocateMemory(long bytes) {
		this._currSize += bytes;
	}

	void releaseMemory(Object obj) {
		long bytes = this.getSize(obj);
		//System.out.println("Releasing " + bytes + " bytes");
		this._currSize -= bytes;
		// Curr size can be less than zero, if store is evicting bigger elements
		// than the ones it registered. We handle this case here.
		if (this._currSize < 0)
			this._currSize = 0;
	}
}	
