package cn.ucai.live.ui.activity;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.hyphenate.chat.EMClient;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.ucai.live.R;
import cn.ucai.live.data.NetDao;
import cn.ucai.live.data.model.Result;
import cn.ucai.live.data.model.Wallet;
import cn.ucai.live.utils.CommonUtils;
import cn.ucai.live.utils.OnCompleteListener;
import cn.ucai.live.utils.PreferenceManager;
import cn.ucai.live.utils.ResultUtils;

public class ChangeActivity extends Activity {
    int change;
    @BindView(R.id.tv_change_balance)
    TextView tvChangeBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change);
        ButterKnife.bind(this);
        initData();
        setChange();
    }

    private void initData() {
        NetDao.loadChange(ChangeActivity.this, EMClient.getInstance().getCurrentUser(),
                new OnCompleteListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        boolean success = false;
                        if (s != null) {
                            Result result = ResultUtils.getResultFromJson(s, Wallet.class);
                            if (result != null && result.isRetMsg()) {
                                success = true;
                                Wallet wallet = (Wallet) result.getRetData();
                                PreferenceManager.getInstance().setcurrentuserChange(wallet.getBalance());
                            }
                        }
                        if (!success) {
                            PreferenceManager.getInstance().setcurrentuserChange(0);

                        }
                    }

                    @Override
                    public void onError(String error) {
                        PreferenceManager.getInstance().setcurrentuserChange(0);
                        CommonUtils.showShortToast("error=====" + error);
                    }
                });
    }

    private void setChange() {
        change = PreferenceManager.getInstance().getCurrentuserChange();
        tvChangeBalance.setText("￥"+Float.valueOf(String.valueOf(change)));
    }
}
