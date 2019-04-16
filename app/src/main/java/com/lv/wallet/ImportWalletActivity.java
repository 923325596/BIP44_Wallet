package com.lv.wallet;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.HDUtils;
import org.spongycastle.crypto.digests.SHA512Digest;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.List;

public class ImportWalletActivity extends AppCompatActivity {

    private EditText mInput;
    private TextView mShowAddress;

    private static final int SEED_ITERATIONS = 2048;
    private static final int SEED_KEY_SIZE = 512;

    private Handler mhandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String result = (String) msg.obj;
            mShowAddress.setText(result);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_wallet);
    }

    /**
     * keystore文件导入
     *
     * @param view
     */
    public void importKeystore(View view) {
        String message = mInput.getText().toString();
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "填写内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    ObjectMapper om = new ObjectMapper();
                    WalletFile walletFile = om.readValue(message, WalletFile.class);
                    Credentials c = loadCredentials("123456", walletFile);

                    String publicKey = Numeric.toHexStringNoPrefix(c.getEcKeyPair().getPublicKey());
                    c.getAddress();
                    String privateKey = Numeric.toHexStringNoPrefix(c.getEcKeyPair().getPrivateKey());
                    Log.e("publicKey", publicKey);
                    Log.e("privateKey", privateKey);
                    String address = Keys.toChecksumAddress(walletFile.getAddress());
                    Message msg = Message.obtain();
                    msg.what = 0;
                    msg.obj = address;
                    mhandler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }.start();


    }

    /**
     * 助记词导入
     *
     * @param view
     */
    public void importMnemonics(View view) {
        String message = mInput.getText().toString();
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "填写内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {

                    byte[] seed = generateSeed(message, null);

                    DeterministicKey rootPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed);
                    // 4. 由根私钥生成 第一个HD 钱包
                    DeterministicHierarchy dh = new DeterministicHierarchy(rootPrivateKey);
                    // 5. 定义父路径 H则是加强 imtoken中的eth钱包进过测试发现使用的是此方式生成 bip44
                    List<ChildNumber> parentPath = HDUtils.parsePath("M/44H/60H/0H/0");
                    // 6. 由父路径,派生出第一个子私钥 "new ChildNumber(0)" 表示第一个 （m/44'/60'/0'/0/0）
                    DeterministicKey child = dh.deriveChild(parentPath, true, true, new ChildNumber(0));
                    byte[] privateKeyByte = child.getPrivKeyBytes();

                    ECKeyPair keyPair = ECKeyPair.create(privateKeyByte);
                    Log.i("TAG", "generateBip44Wallet: 钥匙对  私钥 = " + Numeric.toHexStringNoPrefix(keyPair.getPrivateKey()));
                    Log.i("TAG", "generateBip44Wallet: 钥匙对  公钥 = " + Numeric.toHexStringNoPrefix(keyPair.getPublicKey()));

                    WalletFile walletFile = Wallet.createLight("123456", keyPair);

                    String address = Keys.toChecksumAddress(walletFile.getAddress());

                    Message msg = Message.obtain();
                    msg.what = 0;
                    msg.obj = address;
                    mhandler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * 私钥导入
     *
     * @param view
     */
    public void importPrikey(View view) {
        String message = mInput.getText().toString();
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "填写内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {

                    BigInteger pk = Numeric.toBigInt(message);

                    byte[] privateKeyByte = pk.toByteArray();

                    ECKeyPair keyPair = ECKeyPair.create(privateKeyByte);
                    WalletFile walletFile = Wallet.createLight("123456", keyPair);
                    String address = Keys.toChecksumAddress(walletFile.getAddress());

                    Log.e("lv", "地址：" + address);

                    Message msg = Message.obtain();
                    msg.what = 0;
                    msg.obj = address;
                    mhandler.sendMessage(msg);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    /**
     * To create a binary seed from the mnemonic, we use the PBKDF2 function with a
     * mnemonic sentence (in UTF-8 NFKD) used as the password and the string "mnemonic"
     * + passphrase (again in UTF-8 NFKD) used as the salt. The iteration count is set
     * to 2048 and HMAC-SHA512 is used as the pseudo-random function. The length of the
     * derived key is 512 bits (= 64 bytes).
     *
     * @param mnemonic   The input mnemonic which should be 128-160 bits in length containing
     *                   only valid words
     * @param passphrase The passphrase which will be used as part of salt for PBKDF2
     *                   function
     * @return Byte array representation of the generated seed
     */
    public static byte[] generateSeed(String mnemonic, String passphrase) {
        validateMnemonic(mnemonic);
        passphrase = passphrase == null ? "" : passphrase;


        String salt = String.format("mnemonic%s", passphrase);
        PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA512Digest());
        gen.init(mnemonic.getBytes(Charset.forName("UTF-8")), salt.getBytes(Charset.forName("UTF-8")), SEED_ITERATIONS);

        return ((KeyParameter) gen.generateDerivedParameters(SEED_KEY_SIZE)).getKey();
    }

    private static void validateMnemonic(String mnemonic) {
        if (mnemonic == null || mnemonic.trim().isEmpty()) {
            throw new IllegalArgumentException("Mnemonic is required to generate a seed");
        }
    }


    public static Credentials loadCredentials(String password, WalletFile walletFile)
            throws CipherException {
        return Credentials.create(Wallet.decrypt(password, walletFile));
    }


}
