package cn.ucai.live.ui.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import cn.ucai.live.I;
import cn.ucai.live.LiveHelper;
import cn.ucai.live.R;

import com.hyphenate.EMError;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.ucai.live.data.NetDao;
import cn.ucai.live.data.model.Result;
import cn.ucai.live.utils.CommonUtils;
import cn.ucai.live.utils.L;
import cn.ucai.live.utils.MD5;
import cn.ucai.live.utils.OkHttpUtils;
import cn.ucai.live.utils.ResultUtils;

public class RegisterActivity extends BaseActivity {

    private static final java.lang.String TAG =RegisterActivity.class.getSimpleName() ;
    @BindView(R.id.email)
    EditText etUsername;
    @BindView(R.id.password)
    EditText etPassword;
    @BindView(R.id.usernick)
    EditText etUsernick;
    @BindView(R.id.password_confirm)
    EditText etConfirmetPassword;
    @BindView(R.id.register)
    Button register;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    String username,usernick,password;
    ProgressDialog pd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = etUsername.getText().toString().trim();
                usernick = etUsernick.getText().toString().trim();
                password = etPassword.getText().toString().trim();
                String confirm_pwd = etConfirmetPassword.getText().toString().trim();
                if (TextUtils.isEmpty(username)) {
                    CommonUtils.showShortToast(R.string.User_name_cannot_be_empty);
                    etUsername.requestFocus();
                    return;
                } else if (TextUtils.isEmpty(usernick)) {
                    CommonUtils.showShortToast(R.string.User_nick_cannot_be_empty);
                    etUsernick.requestFocus();
                    return;
                } else if (TextUtils.isEmpty(MD5.getMessageDigest(password))) {
                    CommonUtils.showShortToast(R.string.Password_cannot_be_empty);
                    etPassword.requestFocus();
                    return;
                } else if (TextUtils.isEmpty(MD5.getMessageDigest(confirm_pwd))) {
                    CommonUtils.showShortToast(R.string.Confirm_password_cannot_be_empty);
                    etConfirmetPassword.requestFocus();
                    return;
                } else if (!password.equals(confirm_pwd)) {
                    CommonUtils.showShortToast(R.string.Two_input_password);
                    return;
                }
//                if(TextUtils.isEmpty(etUsername.getText()) || TextUtils.isEmpty(etPassword.getText())){
//                    showToast("用户名和密码不能为空");
//                    return;
//                }
                if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(MD5.getMessageDigest(password))) {
                    pd = new ProgressDialog(RegisterActivity.this);
                    pd.setMessage(getResources().getString(R.string.Is_the_registered));
                    pd.show();
                    registerAppServer();
                }
            }
        });
    }
    private void registerAppServer() {
        NetDao.register(this, username, usernick, MD5.getMessageDigest(password), new OkHttpUtils.OnCompleteListener<String>() {
            @Override
            public void onSuccess(String s) {
                L.e(TAG, "register,s======================>" + s);
                if (s != null) {
                    Result result = ResultUtils.getResultFromJson(s, null);
                    if (result != null) {
                        if (result.isRetMsg()) {
                            registerEMServer();
                        } else {
                            pd.dismiss();
                            if (result.getRetCode() == I.MSG_REGISTER_USERNAME_EXISTS) {
                                CommonUtils.showShortToast(R.string.User_already_exists);
                            } else {
                                CommonUtils.showShortToast(R.string.Registration_failed);
                            }
                        }
                    } else {
                        pd.dismiss();
                        CommonUtils.showShortToast(R.string.Registration_failed);
                    }
                } else {
                    pd.dismiss();
                    CommonUtils.showShortToast(R.string.Registration_failed);
                }
            }

            @Override
            public void onError(String error) {
                L.e(TAG,"error===================="+error);
                pd.dismiss();
                CommonUtils.showShortToast(R.string.Registration_failed);
            }
        });

    }
    private void registerEMServer() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    // call method in SDK
                    EMClient.getInstance().createAccount(username, MD5.getMessageDigest(password));
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (!RegisterActivity.this.isFinishing())
                                pd.dismiss();
                            // save current user
                            LiveHelper.getInstance().setCurrentUserName(username);
                            CommonUtils.showShortToast(R.string.Registered_successfully);
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            finish();
                        }
                    });
                } catch (final HyphenateException e) {
                    unRegisterAppSever();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (!RegisterActivity.this.isFinishing())
                                pd.dismiss();
                            int errorCode = e.getErrorCode();
                            if (errorCode == EMError.NETWORK_ERROR) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.network_anomalies), Toast.LENGTH_SHORT).show();
                            } else if (errorCode == EMError.USER_ALREADY_EXIST) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.User_already_exists), Toast.LENGTH_SHORT).show();
                            } else if (errorCode == EMError.USER_AUTHENTICATION_FAILED) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.registration_failed_without_permission), Toast.LENGTH_SHORT).show();
                            } else if (errorCode == EMError.USER_ILLEGAL_ARGUMENT) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.illegal_user_name), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.Registration_failed), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        }).start();

    }

    private void unRegisterAppSever() {
        NetDao.unregister(this, username, new OkHttpUtils.OnCompleteListener<String>() {
            @Override
            public void onSuccess(String result) {
                L.e(TAG, "result===>" + result);
            }

            @Override
            public void onError(String error) {
                L.e(TAG, "error===>" + error);
            }
        });
    }
}
