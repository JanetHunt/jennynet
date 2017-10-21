package org.janeth.jennynet.core;

/** A priority marker which may be used as parameter for certain
 * sending methods of a <code>Connection</code>.
 */
public enum SendPriority {
	Bottom, 
	Lower,
	Low,
	Normal,
	High,
	Higher,
	Top;
	
	public static SendPriority valueOf (int ordinal) {
		SendPriority v;
		switch (ordinal) {
		   case 0 : v = SendPriority.Bottom; break;
		   case 1 : v = SendPriority.Lower; break;
		   case 2 : v = SendPriority.Low; break;
		   case 3 : v = SendPriority.Normal; break;
		   case 4 : v = SendPriority.High; break;
		   case 5 : v = SendPriority.Higher; break;
		   case 6 : v = SendPriority.Top; break;
		   default: throw new IllegalArgumentException("undefined ordinal value: " + ordinal);
		   }
		   return v;
		}
	
}
