package com.hotbitmapgg.bilibili.module.home.bangumi;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.hotbitmapgg.bilibili.adapter.section.HomeBangumiBannerSection;
import com.hotbitmapgg.bilibili.adapter.section.HomeBangumiBobySection;
import com.hotbitmapgg.bilibili.adapter.section.HomeBangumiItemSection;
import com.hotbitmapgg.bilibili.adapter.section.HomeBangumiNewSerialSection;
import com.hotbitmapgg.bilibili.adapter.section.HomeBangumiRecommendSection;
import com.hotbitmapgg.bilibili.adapter.section.HomeBangumiSeasonNewSection;
import com.hotbitmapgg.bilibili.base.RxLazyFragment;
import com.hotbitmapgg.bilibili.entity.bangumi.BangumiAppIndexInfo;
import com.hotbitmapgg.bilibili.entity.bangumi.BangumiRecommendInfo;
import com.hotbitmapgg.bilibili.network.RetrofitHelper;
import com.hotbitmapgg.bilibili.utils.SnackbarUtil;
import com.hotbitmapgg.bilibili.widget.CustomEmptyView;
import com.hotbitmapgg.bilibili.widget.banner.BannerEntity;
import com.hotbitmapgg.bilibili.widget.sectioned.SectionedRecyclerViewAdapter;
import com.hotbitmapgg.ohmybilibili.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by hcc on 16/8/4 21:18
 * 100332338@qq.com
 * <p/>
 * 首页番剧界面
 */
public class HomeBangumiFragment extends RxLazyFragment {
    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;//下拉刷新内容的容器控件
    @BindView(R.id.recycle)
    RecyclerView mRecyclerView;//用于显示推荐页面的内容,包含:顶部的广告轮播,内容项列表(标题、3列视频项、内容栏底部)
    @BindView(R.id.empty_layout)
    CustomEmptyView mCustomEmptyView;//加载失败时用于显示错误信息和显示失败图片

    private int season;
    private boolean mIsRefreshing = false;
    private List<BannerEntity> bannerList = new ArrayList<>();//广告轮播对象集合
    private SectionedRecyclerViewAdapter mSectionedRecyclerViewAdapter;//RecyclerView控件的适配器
    private List<BangumiRecommendInfo.ResultBean> bangumiRecommends = new ArrayList<>();
    private List<BangumiAppIndexInfo.ResultBean.AdBean.HeadBean> banners = new ArrayList<>();
    private List<BangumiAppIndexInfo.ResultBean.AdBean.BodyBean> bangumibobys = new ArrayList<>();
    private List<BangumiAppIndexInfo.ResultBean.PreviousBean.ListBean> seasonNewBangumis = new ArrayList<>();
    private List<BangumiAppIndexInfo.ResultBean.SerializingBean> newBangumiSerials = new ArrayList<>();

    public static HomeBangumiFragment newInstance() {
        return new HomeBangumiFragment();
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_home_bangumi;
    }

    @Override
    public void finishCreateView(Bundle state) {
        isPrepared = true;
        lazyLoad();
    }

    @Override
    protected void lazyLoad() {
        if (!isPrepared || !isVisible) {
            return;
        }
        initRefreshLayout();
        initRecyclerView();
        isPrepared = false;
    }


    @Override
    protected void initRecyclerView() {
        mSectionedRecyclerViewAdapter = new SectionedRecyclerViewAdapter();
        GridLayoutManager mGridLayoutManager = new GridLayoutManager(getActivity(), 3);
        mGridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (mSectionedRecyclerViewAdapter.getSectionItemViewType(position)) {
                    case SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER:
                        return 3;
                    default:
                        return 1;
                }
            }
        });
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setNestedScrollingEnabled(true);
        mRecyclerView.setLayoutManager(mGridLayoutManager);
        mRecyclerView.setAdapter(mSectionedRecyclerViewAdapter);
        setRecycleNoScroll();
    }


    @Override
    protected void initRefreshLayout() {
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        mSwipeRefreshLayout.post(() -> {
            mSwipeRefreshLayout.setRefreshing(true);
            mIsRefreshing = true;
            loadData();
        });
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            clearData();
            loadData();
        });
    }

    private void clearData() {
        mIsRefreshing = true;
        banners.clear();
        bannerList.clear();
        bangumibobys.clear();
        bangumiRecommends.clear();
        newBangumiSerials.clear();
        seasonNewBangumis.clear();
        mSectionedRecyclerViewAdapter.removeAllSections();
    }


    @Override
    protected void loadData() {
        RetrofitHelper.getBangumiAPI()
                .getBangumiAppIndex()
                .compose(bindToLifecycle())
                .flatMap(new Func1<BangumiAppIndexInfo, Observable<BangumiRecommendInfo>>() {
                    @Override
                    public Observable<BangumiRecommendInfo> call(BangumiAppIndexInfo bangumiAppIndexInfo)
                    {//将getBangumiAPI().getBangumiAppIndex()请求结果保存到下面变量中
                        /*by="Aweigh" date="2018/5/23 16:11"
                          广告分两部分:bangumiAppIndexInfo.getResult().getAd()中的getHead()和getBody()分别表示这两部分广告
                          getHead():是头部广告(位置在页面顶部)
                          getBody():表示广告体,位置在"新番连载"Section之后
                        */
                        banners.addAll(bangumiAppIndexInfo.getResult().getAd().getHead());//对应json xpath=result/ad/head
                        newBangumiSerials.addAll(bangumiAppIndexInfo.getResult().getSerializing());//番剧新番连载Section,对应json xpath=result/serializing
                        bangumibobys.addAll(bangumiAppIndexInfo.getResult().getAd().getBody());//对应json xpath=result/ad/body

                        season = bangumiAppIndexInfo.getResult().getPrevious().getSeason();//对应json xpath=result/previous/season
                        seasonNewBangumis.addAll(bangumiAppIndexInfo.getResult().getPrevious().getList());//番剧分季新番Section,对应json xpath=result/previous/list

                        return RetrofitHelper.getBangumiAPI().getBangumiRecommended();//发起二级请求
                    }
                })
                .compose(bindToLifecycle())
                .map(BangumiRecommendInfo::getResult)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(resultBeans -> {
                    bangumiRecommends.addAll(resultBeans);//getBangumiAPI().getBangumiRecommended()请求结果保存到bangumiRecommends变量中=>番剧推荐Section
                    finishTask();
                }, throwable -> initEmptyView());
    }


    @Override
    protected void finishTask() {
        mSwipeRefreshLayout.setRefreshing(false);
        mIsRefreshing = false;
        hideEmptyView();
        Observable.from(banners)
                .compose(bindToLifecycle())
                .forEach(bannersBean -> bannerList.add(new BannerEntity(
                        bannersBean.getLink(), bannersBean.getTitle(), bannersBean.getImg())));//bannerList对应json xpath=result/ad/head

        mSectionedRecyclerViewAdapter.addSection(new HomeBangumiBannerSection(bannerList));//番剧界面轮播图Section(头部广告)
        mSectionedRecyclerViewAdapter.addSection(new HomeBangumiItemSection(getActivity()));//番剧顶部追番，放送表，索引条目Section
        mSectionedRecyclerViewAdapter.addSection(new HomeBangumiNewSerialSection(getActivity(), newBangumiSerials));//番剧新番连载Section
        if (!bangumibobys.isEmpty()) {
            mSectionedRecyclerViewAdapter.addSection(new HomeBangumiBobySection(getActivity(), bangumibobys));//番剧界面内容Section(内容广告)
        }
        mSectionedRecyclerViewAdapter.addSection(new HomeBangumiSeasonNewSection(getActivity(), season, seasonNewBangumis));//番剧分季新番Section
        mSectionedRecyclerViewAdapter.addSection(new HomeBangumiRecommendSection(getActivity(), bangumiRecommends));//番剧推荐Section
        mSectionedRecyclerViewAdapter.notifyDataSetChanged();
    }


    public void initEmptyView() {
        mSwipeRefreshLayout.setRefreshing(false);
        mCustomEmptyView.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
        mCustomEmptyView.setEmptyImage(R.drawable.img_tips_error_load_error);
        mCustomEmptyView.setEmptyText("加载失败~(≧▽≦)~啦啦啦.");
        SnackbarUtil.showMessage(mRecyclerView, "数据加载失败,请重新加载或者检查网络是否链接");
    }


    public void hideEmptyView() {
        mCustomEmptyView.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    private void setRecycleNoScroll() {
        mRecyclerView.setOnTouchListener((v, event) -> mIsRefreshing);
    }
}
