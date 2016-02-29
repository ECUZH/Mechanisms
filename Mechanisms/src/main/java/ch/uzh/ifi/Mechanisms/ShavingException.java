package ch.uzh.ifi.Mechanisms;

public class ShavingException extends Exception
{

	public ShavingException(String str, double value, double shadingFactor)
	{
		super(str);
		_value = value;
		_shadingFactor = shadingFactor;
	}
	
	private double _value;
	private double _shadingFactor;
}
