package org.bitcoin;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.google.common.base.Preconditions;


/**
 * This class holds native methods to handle ECDSA verification.
 * You can find an example library that can be used for this at
 * https://github.com/sipa/secp256k1
 */
public class NativeSecp256k1 {
    public static final boolean enabled;
    private static final long Secp256k1Context; //ref to pointer to context obj
    static {
        boolean isEnabled = true;
        long contextRef = -1;
        try {
            System.loadLibrary("secp256k1");
            contextRef = secp256k1_init_context();
        } catch (UnsatisfiedLinkError e) {
            isEnabled = false;
        }
        enabled = isEnabled;
        Secp256k1Context = contextRef;
    }
    
    private static ThreadLocal<ByteBuffer> nativeECDSABuffer = new ThreadLocal<ByteBuffer>();
    /**
     * Verifies the given secp256k1 signature in native code.
     * Calling when enabled == false is undefined (probably library not loaded)
     * 
     * @param data The data which was signed, must be exactly 32 bytes
     * @param signature The signature
     * @param pub The public key which did the signing
     */
    public static boolean verify(byte[] data, byte[] signature, byte[] pub) {
        Preconditions.checkArgument(data.length == 32 && signature.length <= 520 && pub.length <= 520);

        ByteBuffer byteBuff = nativeECDSABuffer.get();
        if (byteBuff == null) {
            byteBuff = ByteBuffer.allocateDirect(32 + 520 + 520);
            byteBuff.order(ByteOrder.nativeOrder());
            nativeECDSABuffer.set(byteBuff);
        }
        byteBuff.rewind();
        byteBuff.put(data);
        byteBuff.put(signature);
        byteBuff.put(pub);
        return secp256k1_ecdsa_verify(byteBuff, Secp256k1Context, signature.length, pub.length) == 1;
    }

    /**
     * recover the given secp256k1 pubkey in native code.
     * 
     * @param data The data which was signed, must be exactly 32 bytes
     * @param signature The signature
     * @param compressed whether to recover a compressed pubkey
     * @param pub The public key which did the signing
     */
    public static byte[] recoverCompact(byte[] data, byte[] signature,int compressed, int recID) throws NativeSecp256k1Utils.AssertFailException{
        Preconditions.checkArgument(data.length == 32 && signature.length == 64 && (compressed == 0 || compressed == 1));

        ByteBuffer byteBuff = nativeECDSABuffer.get();
        if (byteBuff == null) {
            byteBuff = ByteBuffer.allocateDirect(32 + 64);
            byteBuff.order(ByteOrder.nativeOrder());
            nativeECDSABuffer.set(byteBuff);
        }
        byteBuff.rewind();
        byteBuff.put(data);
        byteBuff.put(signature);

        byte[][] retByteArray = secp256k1_ecdsa_recover_compact(byteBuff, Secp256k1Context, compressed, recID);

        byte[] pubArr = retByteArray[0];
        int pubLen = new BigInteger(new byte[] { retByteArray[1][0] }).intValue();
        int retVal = new BigInteger(new byte[] { retByteArray[1][1] }).intValue();

        NativeSecp256k1Utils.assertEquals(pubArr.length, pubLen, "Got bad signature length.");

        NativeSecp256k1Utils.assertEquals(retVal, retVal, "Failed return value check.");

        return pubArr;
    }

    /**
     * libsecp256k1 Create an ECDSA signature. 
     * 
     * @param data Message hash, 32 bytes
     * @param key Secret key, 32 bytes
     * 
     * Return values
     * @param sig byte array of signature
     */
    
    public static byte[] sign(byte[] data, byte[] sec) throws NativeSecp256k1Utils.AssertFailException{
        Preconditions.checkArgument(data.length == 32 && sec.length <= 32);

        ByteBuffer byteBuff = nativeECDSABuffer.get();
        if (byteBuff == null) {
            byteBuff = ByteBuffer.allocateDirect(32 + 32);
            byteBuff.order(ByteOrder.nativeOrder());
            nativeECDSABuffer.set(byteBuff);
        }
        byteBuff.rewind();
        byteBuff.put(data);
        byteBuff.put(sec);

        byte[][] retByteArray = secp256k1_ecdsa_sign(byteBuff, Secp256k1Context);

        byte[] sigArr = retByteArray[0];
        int sigLen = new BigInteger(new byte[] { retByteArray[1][0] }).intValue();
        int retVal = new BigInteger(new byte[] { retByteArray[1][1] }).intValue();

        NativeSecp256k1Utils.assertEquals(sigArr.length, sigLen, "Got bad signature length.");

        NativeSecp256k1Utils.assertEquals(retVal, retVal, "Failed return value check.");

        return sigArr;
    } 

    /**
     * libsecp256k1 Create a compact ECDSA signature. 
     * 
     * @param data Message hash, 32 bytes
     * @param key Secret key, 32 bytes
     * 
     * Return values
     * @param sig byte array of signature
     * @param recid recovery id
     */
    
    public static byte[][] signCompact(byte[] data, byte[] sec) throws NativeSecp256k1Utils.AssertFailException{
        Preconditions.checkArgument(data.length == 32 && sec.length <= 32);

        ByteBuffer byteBuff = nativeECDSABuffer.get();
        if (byteBuff == null) {
            byteBuff = ByteBuffer.allocateDirect(32 + 32);
            byteBuff.order(ByteOrder.nativeOrder());
            nativeECDSABuffer.set(byteBuff);
        }
        byteBuff.rewind();
        byteBuff.put(data);
        byteBuff.put(sec);

        byte[][] retByteArray = secp256k1_ecdsa_sign_compact(byteBuff, Secp256k1Context);

        byte[] sigArr = retByteArray[0];
        int recID = new BigInteger(new byte[] { retByteArray[1][0] }).intValue();
        int retVal = new BigInteger(new byte[] { retByteArray[1][1] }).intValue();

        //NativeSecp256k1Test.assertEquals(sigArr.length,sigLen, "Got bad signature length." );

        NativeSecp256k1Utils.assertEquals(retVal, retVal, "Failed return value check.");

        return new byte[][] { sigArr , new byte[]{ (byte) recID} };
    } 

    /**
     * libsecp256k1 Pubkey Verify - returns 1 if valid, 0 if invalid
     * 
     * @param pubkey ECDSA Public key, 33 or 65 bytes
     */
    
    public static boolean pubKeyVerify(byte[] pubkey) {
        Preconditions.checkArgument(pubkey.length == 33 || pubkey.length == 65);

        ByteBuffer byteBuff = nativeECDSABuffer.get();
        if (byteBuff == null) {
            byteBuff = ByteBuffer.allocateDirect(pubkey.length);
            byteBuff.order(ByteOrder.nativeOrder());
            nativeECDSABuffer.set(byteBuff);
        }
        byteBuff.rewind();
        byteBuff.put(pubkey);
        return secp256k1_ec_pubkey_verify(byteBuff,Secp256k1Context, pubkey.length) == 1;
    } 

    /**
     * libsecp256k1 Seckey Verify - returns 1 if valid, 0 if invalid
     * 
     * @param seckey ECDSA Secret key, 32 bytes
     */
    
    public static boolean secKeyVerify(byte[] seckey) {
        Preconditions.checkArgument(seckey.length == 32);

        ByteBuffer byteBuff = nativeECDSABuffer.get();
        if (byteBuff == null) {
            byteBuff = ByteBuffer.allocateDirect(seckey.length);
            byteBuff.order(ByteOrder.nativeOrder());
            nativeECDSABuffer.set(byteBuff);
        }
        byteBuff.rewind();
        byteBuff.put(seckey);
        return secp256k1_ec_seckey_verify(byteBuff,Secp256k1Context) == 1;
    } 


    /**
     * libsecp256k1 Compute Pubkey - computes public key from secret key
     * 
     * @param seckey ECDSA Secret key, 32 bytes
     * @param compressed 1 to return compressed key, 0 for uncompressed
     * 
     * Return values
     * @param pubkey ECDSA Public key, 33 or 65 bytes
     */
    
    public static byte[] computePubkey(byte[] seckey, int compressed) throws NativeSecp256k1Utils.AssertFailException{
        Preconditions.checkArgument(seckey.length == 32);

        ByteBuffer byteBuff = nativeECDSABuffer.get();
        if (byteBuff == null) {
            byteBuff = ByteBuffer.allocateDirect(seckey.length);
            byteBuff.order(ByteOrder.nativeOrder());
            nativeECDSABuffer.set(byteBuff);
        }
        byteBuff.rewind();
        byteBuff.put(seckey);

        byte[][] retByteArray = secp256k1_ec_pubkey_create(byteBuff, Secp256k1Context, compressed);

        byte[] pubArr = retByteArray[0];
        int pubLen = new BigInteger(new byte[] { retByteArray[1][0] }).intValue();
        int retVal = new BigInteger(new byte[] { retByteArray[1][1] }).intValue();

        NativeSecp256k1Utils.assertEquals(pubArr.length, pubLen, "Got bad pubkey length.");

        NativeSecp256k1Utils.assertEquals(retVal, retVal, "Failed return value check.");

        return pubArr;
    } 

    /**
     * libsecp256k1 Cleanup - This destroys the secp256k1 context object
     * This should be called at the end of the program for proper cleanup of the context.
     */
    public static void cleanup() {
        secp256k1_destroy_context(Secp256k1Context);
    }

    /**
     * libsecp256k1 Pubkey Decompress - Decompress a public key
     * 
     * @param pubkey ECDSA Public key, 33 or 65 bytes
     */
    
    public static byte[] pubKeyDecompress(byte[] pubkey) throws NativeSecp256k1Utils.AssertFailException{
        Preconditions.checkArgument(pubkey.length == 33 || pubkey.length == 65);

        ByteBuffer byteBuff = nativeECDSABuffer.get();
        if (byteBuff == null) {
            byteBuff = ByteBuffer.allocateDirect(pubkey.length);
            byteBuff.order(ByteOrder.nativeOrder());
            nativeECDSABuffer.set(byteBuff);
        }
        byteBuff.rewind();
        byteBuff.put(pubkey);

        byte[][] retByteArray = secp256k1_ec_pubkey_decompress(byteBuff,Secp256k1Context, pubkey.length);

        byte[] pubArr = retByteArray[0];
        int pubLen = new BigInteger(new byte[] { retByteArray[1][0] }).intValue();
        int retVal = new BigInteger(new byte[] { retByteArray[1][1] }).intValue();

        NativeSecp256k1Utils.assertEquals(pubArr.length, pubLen, "Got bad pubkey length.");

        NativeSecp256k1Utils.assertEquals(retVal, retVal, "Failed return value check.");

        return pubArr;
    } 

    /**
     * libsecp256k1 Secret Key Import - Import a secret key in DER format.
     * 
     * @param seckey DER Sec key
     * @param compressed Compressed format
     */
    public static byte[] secKeyImport(byte[] seckey) throws NativeSecp256k1Utils.AssertFailException{

        ByteBuffer byteBuff = nativeECDSABuffer.get();
        if (byteBuff == null) {
            byteBuff = ByteBuffer.allocateDirect(seckey.length);
            byteBuff.order(ByteOrder.nativeOrder());
            nativeECDSABuffer.set(byteBuff);
        }
        byteBuff.rewind();
        byteBuff.put(seckey);

        byte[][] retByteArray = secp256k1_ec_privkey_import(byteBuff,Secp256k1Context, seckey.length);

        byte[] privArr = retByteArray[0];

        int privLen = (byte) new BigInteger(new byte[] { retByteArray[1][0] }).intValue() & 0xFF;
        int retVal = new BigInteger(new byte[] { retByteArray[1][1] }).intValue();

        NativeSecp256k1Utils.assertEquals(privArr.length, privLen, "Got bad pubkey length.");

        NativeSecp256k1Utils.assertEquals(retVal, retVal, "Failed return value check.");

        return privArr;
    } 

    /**
     * libsecp256k1 Private Key Export - Export a private key in DER format.
     * 
     * @param seckey ECDSA Sec key, 33 or 65 bytes
     * @param compressed Compressed format
     */
    public static byte[] privKeyExport(byte[] privkey, int compressed) throws NativeSecp256k1Utils.AssertFailException{
        Preconditions.checkArgument(privkey.length == 32 && (compressed == 0 || compressed == 1) );

        ByteBuffer byteBuff = nativeECDSABuffer.get();
        if (byteBuff == null) {
            byteBuff = ByteBuffer.allocateDirect(privkey.length);
            byteBuff.order(ByteOrder.nativeOrder());
            nativeECDSABuffer.set(byteBuff);
        }
        byteBuff.rewind();
        byteBuff.put(privkey);

        byte[][] retByteArray = secp256k1_ec_privkey_export(byteBuff,Secp256k1Context, privkey.length, compressed );

        byte[] privArr = retByteArray[0];

        int privLen = (byte) new BigInteger(new byte[] { retByteArray[1][0] }).intValue() & 0xFF;
        int retVal = new BigInteger(new byte[] { retByteArray[1][1] }).intValue();

        NativeSecp256k1Utils.assertEquals(privArr.length, compressed == 1 ? 214 : 279, "Got bad pubkey length.");

        NativeSecp256k1Utils.assertEquals(retVal, retVal, "Failed return value check.");

        return privArr;
    } 

    /**
     * @param byteBuff signature format is byte[32] data,
     *        native-endian int signatureLength, native-endian int pubkeyLength,
     *        byte[signatureLength] signature, byte[pubkeyLength] pub
     * @returns 1 for valid signature, anything else for invalid
     */
    private static native long secp256k1_init_context();
/*
    private static native long secp256k1_ctx_clone(long context);

    private static native long secp256k1_privkey_tweak_add(ByteBuffer byteBuff, long context);

    private static native long secp256k1_privkey_tweak_mul(ByteBuffer byteBuff, long context);

    private static native long secp256k1_pubkey_tweak_add(ByteBuffer byteBuff, long context);

    private static native long secp256k1_pubkey_tweak_mul(ByteBuffer byteBuff, long context);
*/
    private static native void secp256k1_destroy_context(long context); //thread unsafe - need exclusive access to call

    private static native int secp256k1_ecdsa_verify(ByteBuffer byteBuff, long context, int sigLen, int pubLen);

    private static native byte[][] secp256k1_ecdsa_sign(ByteBuffer byteBuff, long context);

    private static native int secp256k1_ec_seckey_verify(ByteBuffer byteBuff, long context);

    private static native int secp256k1_ec_pubkey_verify(ByteBuffer byteBuff, long context, int pubLen);

    private static native byte[][] secp256k1_ec_pubkey_create(ByteBuffer byteBuff, long context, int compressed);

    private static native byte[][] secp256k1_ec_pubkey_decompress(ByteBuffer byteBuff, long context, int pubLen);

    private static native byte[][] secp256k1_ec_privkey_export(ByteBuffer byteBuff, long context, int privLen, int compressed);

    private static native byte[][] secp256k1_ec_privkey_import(ByteBuffer byteBuff, long context, int privLen);

    private static native byte[][] secp256k1_ecdsa_recover_compact(ByteBuffer byteBuff, long context, int compressed, int recID);

    private static native byte[][] secp256k1_ecdsa_sign_compact(ByteBuffer byteBuff, long context);

    // TODO
    // thread exclusivity
    // randomize() - thread unsafe - need exclusive access from all threads to call
}
