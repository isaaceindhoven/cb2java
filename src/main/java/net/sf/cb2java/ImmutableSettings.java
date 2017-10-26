package net.sf.cb2java;

import net.sf.cb2java.types.SignPosition;

public class ImmutableSettings implements Settings {

	private final String encoding;
	private final Values values;
	private final boolean littleEndian;
	private final String floatConversion;
	private final SignPosition signPosition;
	private final int columnStart;
	private final int columnEnd;

	public ImmutableSettings(String encoding, Values values, boolean littleEndian, String floatConversion, SignPosition signPosition, int columnStart, int columnEnd) {
		this.encoding = encoding;
		this.values = values;
		this.littleEndian = littleEndian;
		this.floatConversion = floatConversion;
		this.signPosition = signPosition;
		this.columnStart = columnStart;
		this.columnEnd = columnEnd;
	}

	@Override
	public String getEncoding() {
		return encoding;
	}

	@Override
	public Values getValues() {
		return values;
	}

	@Override
	public boolean getLittleEndian() {
		return littleEndian;
	}

	@Override
	public String getFloatConversion() {
		return floatConversion;
	}

	@Override
	public SignPosition getSignPosition() {
		return signPosition;
	}

	@Override
	public int getColumnStart() {
		return columnStart;
	}

	@Override
	public int getColumnEnd() {
		return columnEnd;
	}

}
