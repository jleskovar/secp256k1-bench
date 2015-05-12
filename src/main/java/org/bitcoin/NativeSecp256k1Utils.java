package org.bitcoin;

public class NativeSecp256k1Utils {

    public static void assertEquals( int val, int val2, String message ) throws AssertFailException{
      if( val != val2 )
        throw new AssertFailException("FAIL: " + message);
    }

    private static void assertEquals( boolean val, boolean val2, String message ) throws AssertFailException{
      if( val != val2 ) 
        throw new AssertFailException("FAIL: " + message);
      else
        System.out.println("PASS: " + message);
    }

    private static void assertEquals( String val, String val2, String message ) throws AssertFailException{
      if( !val.equals(val2) ) 
        throw new AssertFailException("FAIL: " + message);
      else
        System.out.println("PASS: " + message);
    }

    public static class AssertFailException extends Exception {
      public AssertFailException(String message) {
        super( message );
      }
    }
}
