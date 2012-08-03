package ru.nacu.commons.tabs;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.TabHost;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Активити, которое содержит несколько табов (которые в свою очередь являются фрагментами)
 *
 * @author quadro
 * @since 7/2/12 1:42 PM
 */
public abstract class TabFragmentActivity extends SherlockFragmentActivity implements TabHost.OnTabChangeListener {

    private final int layout;
    private final int realTablContentId;

    private TabHost mTabHost;
    private Map<String, TabInfo> mapTabInfo = new HashMap<String, TabInfo>();
    private TabInfo mLastTab = null;

    public TabFragmentActivity(int layout, int realTablContentId) {
        this.layout = layout;
        this.realTablContentId = realTablContentId;
    }

    protected static class TabInfo {
        private View label;
        private String tag;
        private Class clss;
        private Bundle args;
        private Fragment fragment;

        public TabInfo(View label, String tag, Class clazz, Bundle args) {
            this.label = label;
            this.tag = tag;
            this.clss = clazz;
            this.args = args;
        }
    }

    class TabFactory implements TabHost.TabContentFactory {

        private final Context mContext;

        /**
         * @param context context
         */
        public TabFactory(Context context) {
            mContext = context;
        }

        /**
         * (non-Javadoc)
         *
         * @see android.widget.TabHost.TabContentFactory#createTabContent(java.lang.String)
         */
        public View createTabContent(String tag) {
            View v = new View(mContext);
            v.setMinimumWidth(0);
            v.setMinimumHeight(0);
            return v;
        }
    }

    /**
     * (non-Javadoc)
     *
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Step 1: Inflate layout
        setContentView(layout);
        // Step 2: Setup TabHost
        initialiseTabHost(savedInstanceState);
        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab")); //set the tab as per the saved state
        }
    }

    /**
     * (non-Javadoc)
     *
     * @see android.support.v4.app.FragmentActivity#onSaveInstanceState(android.os.Bundle)
     */
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("tab", mTabHost.getCurrentTabTag()); //save the tab selected
        super.onSaveInstanceState(outState);
    }

    /**
     * Step 2: Setup TabHost
     */
    private void initialiseTabHost(Bundle args) {
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();

        final List<TabInfo> tabs = getTabs(args);
        for (TabInfo info : tabs) {
            addTab(this, this.mTabHost, this.mTabHost.newTabSpec(info.tag).setIndicator(info.label), info);
            mapTabInfo.put(info.tag, info);
        }
        // Default to first tab
        onTabChanged(tabs.get(0).tag);
        mTabHost.setOnTabChangedListener(this);
    }

    protected abstract List<TabInfo> getTabs(Bundle args);

    private static void addTab(TabFragmentActivity activity, TabHost tabHost, TabHost.TabSpec tabSpec, TabInfo tabInfo) {
        // Attach a Tab view factory to the spec
        tabSpec.setContent(activity.new TabFactory(activity));
        String tag = tabSpec.getTag();

        // Check to see if we already have a fragment for this tab, probably
        // from a previously saved state.  If so, deactivate it, because our
        // initial state is that a tab isn't shown.
        tabInfo.fragment = activity.getSupportFragmentManager().findFragmentByTag(tag);
        if (tabInfo.fragment != null && !tabInfo.fragment.isDetached()) {
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            ft.detach(tabInfo.fragment);
            ft.commit();
            activity.getSupportFragmentManager().executePendingTransactions();
        }

        tabHost.addTab(tabSpec);
    }

    protected void selectTab(int tab) {
        mTabHost.setCurrentTab(tab);
    }

    /**
     * (non-Javadoc)
     *
     * @see android.widget.TabHost.OnTabChangeListener#onTabChanged(java.lang.String)
     */
    public void onTabChanged(String tag) {
        TabInfo newTab = (TabInfo) this.mapTabInfo.get(tag);
        if (mLastTab != newTab) {
            FragmentTransaction ft = this.getSupportFragmentManager().beginTransaction();
            if (mLastTab != null) {
                if (mLastTab.fragment != null) {
                    ft.detach(mLastTab.fragment);
                }
            }
            if (newTab != null) {
                if (newTab.fragment == null) {
                    newTab.fragment = Fragment.instantiate(this,
                            newTab.clss.getName(), newTab.args);
                    ft.add(realTablContentId, newTab.fragment, newTab.tag);
                } else {
                    ft.attach(newTab.fragment);
                }
            }

            mLastTab = newTab;
            ft.commit();
            this.getSupportFragmentManager().executePendingTransactions();
        }
    }
}
