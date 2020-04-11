package in.lubble.app.network;

import java.util.ArrayList;
import java.util.List;

import in.lubble.app.auth.LocationsData;
import in.lubble.app.explore.ExploreGroupData;
import in.lubble.app.marketplace.ItemSearchData;
import in.lubble.app.marketplace.RatingData;
import in.lubble.app.marketplace.SliderData;
import in.lubble.app.models.AirtableCollectionData;
import in.lubble.app.models.AirtablePlacesData;
import in.lubble.app.models.EventData;
import in.lubble.app.models.EventIdData;
import in.lubble.app.models.FeatureData;
import in.lubble.app.models.airtable_pojo.AirtableBooksData;
import in.lubble.app.models.airtable_pojo.AirtableBooksRecord;
import in.lubble.app.models.marketplace.Category;
import in.lubble.app.models.marketplace.Item;
import in.lubble.app.models.marketplace.MarketplaceData;
import in.lubble.app.models.marketplace.SellerData;
import in.lubble.app.models.pojos.BooksData;
import in.lubble.app.profile.UserProfileData;
import in.lubble.app.quiz.PlaceData;
import in.lubble.app.quiz.QuestionData;
import in.lubble.app.referrals.ReferralHistoryData;
import in.lubble.app.referrals.ReferralLeaderboardData;
import in.lubble.app.rewards.data.RewardCodesAirtableData;
import in.lubble.app.rewards.data.RewardsAirtableData;
import in.lubble.app.utils.YoutubeData;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface Endpoints {

    @GET("marketplace/home/")
    Call<MarketplaceData> fetchMarketplaceData();

    @GET("marketplace/categories/")
    Call<ArrayList<Category>> fetchCategories();

    @POST("marketplace/item/")
    Call<Item> uploadNewItem(@Body RequestBody params);

    @PUT("marketplace/item/{item_id}/")
    Call<Item> updateItem(@Path("item_id") int itemId, @Body RequestBody params);

    @GET("marketplace/item/{item_id}/")
    Call<Item> fetchItemDetails(@Path("item_id") int itemId);

    @POST("marketplace/seller/")
    Call<SellerData> uploadSellerProfile(@Body RequestBody params);

    @GET("marketplace/seller/{seller_id}/")
    Call<ArrayList<SellerData>> fetchSellerProfile(@Path("seller_id") int sellerId);

    @PUT("marketplace/seller/{seller_id}/")
    Call<SellerData> updateSellerProfile(@Path("seller_id") int sellerId, @Body RequestBody params);

    @GET("app/features/")
    Call<FeatureData> fetchAppFeatures();

    @GET("marketplace/items/category/{cat_id}/")
    Call<Category> fetchCategoryItems(@Path("cat_id") int categoryId);

    @GET("marketplace/items/seller/{seller_id}/")
    Call<SellerData> fetchSellerItems(@Path("seller_id") int sellerId);

    @GET("marketplace/items/seller/{seller_name}/")
    Call<SellerData> fetchSellerItems(@Path("seller_name") String sellerName);

    @GET("marketplace/itemlist/")
    Call<ArrayList<ItemSearchData>> fetchItemList();

    @POST("marketplace/item/{item_id}/rating/")
    Call<RatingData> uploadNewRating(@Path("item_id") int itemId, @Body RequestBody params);

    @PUT("marketplace/rating/{rating_id}/")
    Call<RatingData> updateRating(@Path("rating_id") int ratingId, @Body RequestBody params);

    @POST("marketplace/seller/{seller_id}/recommend/")
    Call<RatingData> uploadRecommendation(@Path("seller_id") int sellerId);

    @DELETE("marketplace/seller/{seller_id}/recommend/")
    Call<RatingData> deleteRecommendation(@Path("seller_id") int sellerId);

    @GET("marketplace/allitems/")
    Call<ArrayList<Item>> fetchAllItems();

    @GET("marketplace/servicecategories/")
    Call<ArrayList<Category>> fetchServiceCategories();

    @GET("referral/history/")
    Call<ReferralHistoryData> fetchReferralHistory();

    @GET("referral/leaderboard/")
    Call<ReferralLeaderboardData> fetchReferralLeaderboard();

    @POST("signup/")
    Call<ArrayList<LocationsData>> uploadSignUp(@Body RequestBody params);

    @POST("signup_complete/")
    Call<Void> uploadSignUpComplete(@Body RequestBody params);

    @GET("home/")
    Call<ArrayList<SliderData>> fetchHomeData();

    @GET("explore/{lubble_id}/")
    Call<ArrayList<ExploreGroupData>> fetchExploreGroups(@Path("lubble_id") String lubbleId);

    @GET("profile/{user_id}/")
    Call<UserProfileData> fetchUserProfile(@Path("user_id") String userId);

    @GET
    public Call<YoutubeData> getYoutubeData(@Url String url);

    @GET("quiz/")
    public Call<ArrayList<QuestionData>> getQuizQuestions();

    @POST("quiz/")
    public Call<PlaceData> getQuizResult(@Body RequestBody params);

    @GET
    public Call<AirtableData> fetchMore(@Url String url);

    @GET
    public Call<AirtableCollectionData> fetchEntries(@Url String url);

    @GET
    public Call<AirtablePlacesData> fetchPlaces(@Url String url);

    @GET
    public Call<BooksData> searchBooks(@Url String url);

    @GET
    public Call<AirtableBooksData> fetchBooks(@Url String url);

    @POST
    public Call<AirtableBooksRecord> uploadNewBook(@Url String url, @Body RequestBody params);

    @POST
    public Call<AirtableBooksRecord> uploadNewOrder(@Url String url, @Body RequestBody params);

    @GET
    public Call<RewardsAirtableData> fetchRewards(@Url String url);

    @GET
    public Call<RewardCodesAirtableData> fetchRewardCodes(@Url String url);

    @PATCH
    public Call<RewardsAirtableData> uploadRewardClaim(@Url String url, @Body RequestBody params);

    @POST
    public Call<RewardsAirtableData> uploadInstalledApps(@Url String url, @Body RequestBody params);

    @GET("marketplace/events/list/")
    Call<List<EventData>> getEvents(@Query("lubble_id")String lubble_id);

    @GET("marketplace/events/")
    Call<List<EventData>> getEvent(@Query("event_id")String event_id);

    @POST("marketplace/events/attendee/")
    Call<Void> uploadattendee(@Body RequestBody params);

    @POST("marketplace/events/")
    Call<EventIdData> upload_new_event(@Body RequestBody params);
}
