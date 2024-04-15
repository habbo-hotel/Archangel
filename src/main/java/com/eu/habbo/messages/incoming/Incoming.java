package com.eu.habbo.messages.incoming;

import com.eu.habbo.messages.incoming.achievements.GetAchievementsEvent;
import com.eu.habbo.messages.incoming.achievements.RequestAchievementConfigurationEvent;
import com.eu.habbo.messages.incoming.ambassadors.AmbassadorAlertEvent;
import com.eu.habbo.messages.incoming.ambassadors.FollowFriendEvent;
import com.eu.habbo.messages.incoming.camera.*;
import com.eu.habbo.messages.incoming.campaign.OpenCampaignCalendarDoorAsStaffEvent;
import com.eu.habbo.messages.incoming.campaign.OpenCampaignCalendarDoorEvent;
import com.eu.habbo.messages.incoming.catalog.*;
import com.eu.habbo.messages.incoming.catalog.marketplace.*;
import com.eu.habbo.messages.incoming.catalog.recycler.GetRecyclerPrizesEvent;
import com.eu.habbo.messages.incoming.catalog.recycler.GetRecyclerStatusEvent;
import com.eu.habbo.messages.incoming.catalog.recycler.PresentOpenEvent;
import com.eu.habbo.messages.incoming.catalog.recycler.RecycleItemsEvent;
import com.eu.habbo.messages.incoming.crafting.*;
import com.eu.habbo.messages.incoming.floorplaneditor.GetOccupiedTilesEvent;
import com.eu.habbo.messages.incoming.floorplaneditor.GetRoomEntryTileEvent;
import com.eu.habbo.messages.incoming.floorplaneditor.UpdateFloorPropertiesEvent;
import com.eu.habbo.messages.incoming.friends.*;
import com.eu.habbo.messages.incoming.gamecenter.*;
import com.eu.habbo.messages.incoming.guardians.ChatReviewGuideDecidesOnOfferEvent;
import com.eu.habbo.messages.incoming.guardians.ChatReviewGuideDetachedEvent;
import com.eu.habbo.messages.incoming.guardians.ChatReviewGuideVoteEvent;
import com.eu.habbo.messages.incoming.guides.*;
import com.eu.habbo.messages.incoming.guild.*;
import com.eu.habbo.messages.incoming.guild.forums.*;
import com.eu.habbo.messages.incoming.handshake.*;
import com.eu.habbo.messages.incoming.helper.GetCfhStatusEvent;
import com.eu.habbo.messages.incoming.helper.GetTalentTrackEvent;
import com.eu.habbo.messages.incoming.hotelview.*;
import com.eu.habbo.messages.incoming.inventory.*;
import com.eu.habbo.messages.incoming.modtool.*;
import com.eu.habbo.messages.incoming.navigator.*;
import com.eu.habbo.messages.incoming.polls.AnswerPollEvent;
import com.eu.habbo.messages.incoming.polls.PollRejectEvent;
import com.eu.habbo.messages.incoming.polls.PollStartEvent;
import com.eu.habbo.messages.incoming.rooms.*;
import com.eu.habbo.messages.incoming.rooms.bots.CommandBotEvent;
import com.eu.habbo.messages.incoming.rooms.bots.GetBotCommandConfigurationDataEvent;
import com.eu.habbo.messages.incoming.rooms.bots.PlaceBotEvent;
import com.eu.habbo.messages.incoming.rooms.bots.RemoveBotFromFlatEvent;
import com.eu.habbo.messages.incoming.rooms.items.*;
import com.eu.habbo.messages.incoming.rooms.items.jukebox.*;
import com.eu.habbo.messages.incoming.rooms.items.lovelock.FriendFurniConfirmLockEvent;
import com.eu.habbo.messages.incoming.rooms.items.rentablespace.RentableSpaceCancelRentEvent;
import com.eu.habbo.messages.incoming.rooms.items.rentablespace.RentableSpaceRentEvent;
import com.eu.habbo.messages.incoming.rooms.items.youtube.ControlYoutubeDisplayPlaybackEvent;
import com.eu.habbo.messages.incoming.rooms.items.youtube.GetYoutubeDisplayStatusEvent;
import com.eu.habbo.messages.incoming.rooms.items.youtube.SetYoutubeDisplayPlaylistEvent;
import com.eu.habbo.messages.incoming.rooms.pets.*;
import com.eu.habbo.messages.incoming.rooms.promotions.EditEventEvent;
import com.eu.habbo.messages.incoming.rooms.promotions.GetRoomAdPurchaseInfoEvent;
import com.eu.habbo.messages.incoming.rooms.promotions.PurchaseRoomAdEvent;
import com.eu.habbo.messages.incoming.rooms.users.*;
import com.eu.habbo.messages.incoming.trading.*;
import com.eu.habbo.messages.incoming.unknown.GetResolutionAchievementsEvent;
import com.eu.habbo.messages.incoming.unknown.UnknownEvent2;
import com.eu.habbo.messages.incoming.users.*;
import com.eu.habbo.messages.incoming.wired.ApplySnapshotEvent;
import com.eu.habbo.messages.incoming.wired.UpdateActionEvent;
import com.eu.habbo.messages.incoming.wired.UpdateConditionEvent;
import com.eu.habbo.messages.incoming.wired.UpdateTriggerEvent;
import com.eu.habbo.roleplay.messages.incoming.combat.AttackUserEvent;
import com.eu.habbo.roleplay.messages.incoming.combat.EquipWeaponEvent;
import com.eu.habbo.roleplay.messages.incoming.combat.ListMyWeaponsEvent;
import com.eu.habbo.roleplay.messages.incoming.corp.*;
import com.eu.habbo.roleplay.messages.incoming.game.TimeOfDayQueryEvent;
import com.eu.habbo.roleplay.messages.incoming.gang.*;
import com.eu.habbo.roleplay.messages.incoming.police.*;
import com.eu.habbo.roleplay.messages.incoming.users.UserRoleplayStatsQueryComposer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Incoming {
    acceptFriendEvent(137, AcceptFriendEvent.class),
    acceptTradingEvent(3863, AcceptTradingEvent.class),
    addAdminRightsToMemberEvent(2894, AddAdminRightsToMemberEvent.class),
    addFavouriteRoomEvent(3817, AddFavouriteRoomEvent.class),
    addItemToTradeEvent(3107, AddItemToTradeEvent.class),
    addItemsToTradeEvent(1263, AddItemsToTradeEvent.class),
    addJukeboxDiskEvent(753, AddJukeboxDiskEvent.class),
    addSpamWallPostItEvent(3283, AddSpamWallPostItEvent.class),
    ambassadorAlertEvent(2996, AmbassadorAlertEvent.class),
    answerPollEvent(3505, AnswerPollEvent.class),
    applySnapshotEvent(3373, ApplySnapshotEvent.class),
    approveMembershipRequestEvent(3386, ApproveMembershipRequestEvent.class),
    approveNameEvent(2109, ApproveNameEvent.class),
    assignRightsEvent(808, AssignRightsEvent.class),
    avatarEffectActivatedEvent(2959, AvatarEffectActivatedEvent.class),
    avatarEffectSelectedEvent(1752, AvatarEffectSelectedEvent.class),
    avatarExpressionEvent(2456, AvatarExpressionEvent.class),
    banUserWithDurationEvent(1477, BanUserWithDurationEvent.class),
    breedPetsEvent(1638, BreedPetsEvent.class),
    buildersClubPlaceWallItemEvent(2462, BuildersClubPlaceWallItemEvent.class),
    buildersClubQueryFurniCountEvent(2529, BuildersClubQueryFurniCountEvent.class),
    buyMarketplaceOfferEvent(1603, BuyMarketplaceOfferEvent.class),
    callForHelpEvent(1691, CallForHelpEvent.class),
    callForHelpFromForumMessageEvent(1412, CallForHelpFromForumMessageEvent.class),
    callForHelpFromForumThreadEvent(534, CallForHelpFromForumThreadEvent.class),
    callForHelpFromIMEvent(2950, CallForHelpFromIMEvent.class),
    callForHelpFromPhotoEvent(2492, CallForHelpFromPhotoEvent.class),
    canCreateRoomEvent(2128, CanCreateRoomEvent.class),
    cancelMarketplaceOfferEvent(434, CancelMarketplaceOfferEvent.class),
    cancelPetBreedingEvent(2713, CancelPetBreedingEvent.class),
    cancelTypingEvent(1474, CancelTypingEvent.class),
    changeMottoEvent(2228, ChangeMottoEvent.class),
    changePostureEvent(2235, ChangePostureEvent.class),
    changeUserNameEvent(2977, ChangeUserNameEvent.class),
    chatEvent(1314, ChatEvent.class),
    chatReviewGuideDecidesOnOfferEvent(3365, ChatReviewGuideDecidesOnOfferEvent.class),
    chatReviewGuideDetachedEvent(2501, ChatReviewGuideDetachedEvent.class),
    chatReviewGuideVoteEvent(3961, ChatReviewGuideVoteEvent.class),
    chatReviewSessionCreateEvent(3060, ChatReviewSessionCreateEvent.class),
    checkUserNameEvent(3950, CheckUserNameEvent.class),
    clientHelloEvent(4000, ClientHelloEvent.class),
    closeIssueDefaultActionEvent(2717, CloseIssueDefaultActionEvent.class),
    closeIssuesEvent(2067, CloseIssuesEvent.class),
    closeTradingEvent(2551, CloseTradingEvent.class),
    commandBotEvent(2624, CommandBotEvent.class),
    completeDiffieHandshakeEvent(773, CompleteDiffieHandshakeEvent.class),
    compostPlantEvent(3835, CompostPlantEvent.class),
    confirmAcceptTradingEvent(2760, ConfirmAcceptTradingEvent.class),
    confirmDeclineTradingEvent(2341, ConfirmDeclineTradingEvent.class),
    confirmPetBreedingEvent(3382, ConfirmPetBreedingEvent.class),
    controlYoutubeDisplayPlaybackEvent(3005, ControlYoutubeDisplayPlaybackEvent.class),
    craftEvent(3591, CraftEvent.class),
    craftSecretEvent(1251, CraftSecretEvent.class),
    createFlatEvent(2752, CreateFlatEvent.class),
    createGuildEvent(230, CreateGuildEvent.class),
    creditFurniRedeemEvent(3115, CreditFurniRedeemEvent.class),
    customizeAvatarWithFurniEvent(3374, CustomizeAvatarWithFurniEvent.class),
    customizePetWithFurniEvent(1328, CustomizePetWithFurniEvent.class),
    danceEvent(2080, DanceEvent.class),
    deactivateGuildEvent(1134, DeactivateGuildEvent.class),
    declineFriendEvent(2890, DeclineFriendEvent.class),
    deleteFavouriteRoomEvent(309, DeleteFavouriteRoomEvent.class),
    deleteRoomEvent(532, DeleteRoomEvent.class),
    deselectFavouriteHabboGroupEvent(1820, DeselectFavouriteHabboGroupEvent.class),
    diceOffEvent(1533, DiceOffEvent.class),

    dropCarryItemEvent(2814, DropCarryItemEvent.class),

    editEventEvent(3991, EditEventEvent.class),

    enterOneWayDoorEvent(2765, EnterOneWayDoorEvent.class),

    eventLogEvent(3457, EventLogEvent.class),

    findNewFriendsEvent(516, FindNewFriendsEvent.class),

    followFriendEvent(2970, FollowFriendEvent.class),

    forwardToSomeRoomEvent(1703, ForwardToSomeRoomEvent.class),

    friendFurniConfirmLockEvent(3775, FriendFurniConfirmLockEvent.class),

    game2GetAccountGameStatusEvent(11, UnknownEvent2.class),

    gameUnloadedEvent(3207, GameUnloadedEvent.class),

    getAchievementsEvent(219, GetAchievementsEvent.class),

    getBadgePointLimitsEvent(1371, GetBadgePointLimitsEvent.class),

    getBadgesEvent(2769, GetBadgesEvent.class),

    getBannedUsersFromRoomEvent(2267, GetBannedUsersFromRoomEvent.class),

    getBonusRareInfoEvent(957, GetBonusRareInfoEvent.class),

    getBotCommandConfigurationDataEvent(1986, GetBotCommandConfigurationDataEvent.class),

    getBotInventoryEvent(3848, GetBotInventoryEvent.class),

    getBundleDiscountRulesetEvent(223, GetBundleDiscountRulesetEvent.class),

    getCatalogIndexEvent(1195, GetCatalogIndexEvent.class),

    getCatalogPageEvent(412, GetCatalogPageEvent.class),

    getCfhChatlogEvent(211, GetCfhChatlogEvent.class),

    getCfhStatusEvent(2746, GetCfhStatusEvent.class),

    getClubGiftInfo(487, GetClubGiftInfo.class),

    getClubOffersEvent(3285, GetClubOffersEvent.class),

    getCraftableProductsEvent(633, GetCraftableProductsEvent.class),

    getCraftingRecipeEvent(1173, GetCraftingRecipeEvent.class),

    getCraftingRecipesAvailableEvent(3086, GetCraftingRecipesAvailableEvent.class),

    getCreditsInfoEvent(273, GetCreditsInfoEvent.class),

    getCurrentTimingCodeEvent(2912, GetCurrentTimingCodeEvent.class),

    getCustomRoomFilterEvent(1911, GetCustomRoomFilterEvent.class),

    getExtendedProfileEvent(3265, GetExtendedProfileEvent.class),

    getFlatControllersEvent(3385, GetFlatControllersEvent.class),

    getForumStatsEvent(3149, GetForumStatsEvent.class),

    getForumsListEvent(873, GetForumsListEvent.class),

    getFriendRequestsEvent(2448, GetFriendRequestsEvent.class),

    getFurnitureAliasesEvent(3898, GetRoomEntryDataEvent.class),

    getGameListEvent(741, GetGameListEvent.class),

    getGameStatusEvent(3171, GetGameStatusEvent.class),

    getGiftWrappingConfigurationEvent(418, GetGiftWrappingConfigurationEvent.class),

    getGuestRoomEvent(2230, GetGuestRoomEvent.class),

    getGuideReportingStatusEvent(3786, GetGuideReportingStatusEvent.class),

    getGuildCreationInfoEvent(798, GetGuildCreationInfoEvent.class),

    getGuildEditInfoEvent(1004, GetGuildEditInfoEvent.class),

    getGuildEditorDataEvent(813, GetGuildEditorDataEvent.class),

    getGuildFurniContextMenuInfoEvent(2651, GetGuildFurniContextMenuInfoEvent.class),

    getGuildMembersEvent(312, GetGuildMembersEvent.class),

    getGuildMembershipsEvent(367, GetGuildMembershipsEvent.class),

    getHabboGroupBadgesEvent(21, GetHabboGroupBadgesEvent.class),

    getHabboGroupDetailsEvent(2991, GetHabboGroupDetailsEvent.class),

    getIgnoredUsersEvent(3878, GetIgnoredUsersEvent.class),

    getItemDataEvent(3964, GetItemDataEvent.class),

    getJukeboxPlayListEvent(1435, GetJukeboxPlayListEvent.class),

    getLimitedOfferAppearingNextEvent(410, GetLimitedOfferAppearingNextEvent.class),

    getMOTDEvent(1523, GetMOTDEvent.class),

    getMarketplaceCanMakeOfferEvent(848, GetMarketplaceCanMakeOfferEvent.class),

    getMarketplaceConfigurationEvent(2597, GetMarketplaceConfigurationEvent.class),

    getMarketplaceItemStatsEvent(3288, GetMarketplaceItemStatsEvent.class),

    getMarketplaceOffersEvent(2407, GetMarketplaceOffersEvent.class),

    getMarketplaceOwnOffersEvent(2105, GetMarketplaceOwnOffersEvent.class),

    getMemberGuildItemCountEvent(3593, GetMemberGuildItemCountEvent.class),

    getMessagesEvent(232, GetMessagesEvent.class),

    getModeratorRoomInfoEvent(707, GetModeratorRoomInfoEvent.class),

    getModeratorUserInfoEvent(3295, GetModeratorUserInfoEvent.class),

    getNowPlayingEvent(1325, GetNowPlayingEvent.class),

    getOccupiedTilesEvent(1687, GetOccupiedTilesEvent.class),

    getOfficialSongIdEvent(3189, GetOfficialSongIdEvent.class),

    getPendingCallsForHelpEvent(3267, GetPendingCallsForHelpEvent.class),

    getPetCommandsEvent(2161, GetPetCommandsEvent.class),

    getPetInfoEvent(2934, GetPetInfoEvent.class),

    getPetInventoryEvent(3095, GetPetInventoryEvent.class),

    getPopularRoomTagsEvent(826, GetPopularRoomTagsEvent.class),

    getProductOfferEvent(2594, GetProductOfferEvent.class),

    getPromoArticlesEvent(1827, GetPromoArticlesEvent.class),

    getRecyclerPrizesEvent(398, GetRecyclerPrizesEvent.class),

    getRecyclerStatusEvent(1342, GetRecyclerStatusEvent.class),

    getRelationshipStatusInfoEvent(2138, GetRelationshipStatusInfoEvent.class),

    getResolutionAchievementsEvent(359, GetResolutionAchievementsEvent.class),

    getRoomAdPurchaseInfoEvent(1075, GetRoomAdPurchaseInfoEvent.class),

    getRoomChatlogEvent(2587, GetRoomChatlogEvent.class),

    getRoomEntryDataEvent(2300, GetRoomEntryDataEvent.class),

    getRoomEntryTileEvent(3559, GetRoomEntryTileEvent.class),

    getRoomSettingsEvent(3129, GetRoomSettingsEvent.class),

    getRoomVisitsEvent(3526, GetRoomVisitsEvent.class),

    getSelectedBadgesEvent(2091, GetSelectedBadgesEvent.class),

    getSellablePetPalettesEvent(1756, GetSellablePetPalettesEvent.class),

    getSongInfoEvent(3082, GetSongInfoEvent.class),

    getSoundSettingsEvent(2388, GetSoundSettingsEvent.class),

    getTalentTrackEvent(196, GetTalentTrackEvent.class),

    getTalentTrackLevelEvent(2127, GetTalentTrackLevelEvent.class),

    getThreadsEvent(436, GetThreadsEvent.class),

    getUnreadForumsCountEvent(2908, GetUnreadForumsCountEvent.class),

    getUserChatlogEvent(1391, GetUserChatlogEvent.class),

    getUserEventCatsEvent(1782, GetUserEventCatsEvent.class),

    getUserFlatCatsEvent(3027, GetUserFlatCatsEvent.class),

    getUserTagsEvent(17, GetUserTagsEvent.class),

    getWardrobeEvent(2742, GetWardrobeEvent.class),

    getWeeklyGameRewardEvent(2914, GetWeeklyGameRewardEvent.class),

    getWeeklyGameRewardWinnersEvent(1054, GetWeeklyGameRewardWinnersEvent.class),

    getYoutubeDisplayStatusEvent(336, GetYoutubeDisplayStatusEvent.class),

    guideSessionCreateEvent(3338, GuideSessionCreateEvent.class),

    guideSessionFeedbackEvent(477, GuideSessionFeedbackEvent.class),

    guideSessionGetRequesterRoomEvent(1052, GuideSessionGetRequesterRoomEvent.class),

    guideSessionGuideDecidesEvent(1424, GuideSessionGuideDecidesEvent.class),

    guideSessionInviteRequesterEvent(234, GuideSessionInviteRequesterEvent.class),

    guideSessionIsTypingEvent(519, GuideSessionIsTypingEvent.class),

    guideSessionMessageEvent(3899, GuideSessionMessageEvent.class),

    guideSessionOnDutyUpdateEvent(1922, GuideSessionOnDutyUpdateEvent.class),

    guideSessionReportEvent(3969, GuideSessionReportEvent.class),

    guideSessionRequesterCancelsEvent(291, GuideSessionRequesterCancelsEvent.class),

    guideSessionResolvedEvent(887, GuideSessionResolvedEvent.class),

    habboSearchEvent(1210, HabboSearchEvent.class),

    hotelViewClaimBadgeRewardEvent(-1, HotelViewClaimBadgeRewardEvent.class),

    hotelViewRequestBadgeRewardEvent(2318, HotelViewRequestBadgeRewardEvent.class),

    hotelViewRequestSecondsUntilEvent(271, HotelViewRequestSecondsUntilEvent.class),

    ignoreUserEvent(1117, IgnoreUserEvent.class),

    infoRetrieveEvent(357, InfoRetrieveEvent.class),

    initDiffieHandshakeEvent(3110, InitDiffieHandshakeEvent.class),

    joinHabboGroupEvent(998, JoinHabboGroupEvent.class),

    joinQueueEvent(1458, JoinQueueEvent.class),

    jukeBoxEventOne(2304, JukeBoxEventOne.class),

    kickMemberEvent(593, KickMemberEvent.class),

    latencyPingRequestEvent(295, LatencyPingRequestEvent.class),

    letUserInEvent(1644, LetUserInEvent.class),

    lookToEvent(3301, LookToEvent.class),

    makeOfferEvent(3447, MakeOfferEvent.class),

    messengerInitEvent(2781, MessengerInitEvent.class),

    modAlertEvent(229, ModAlertEvent.class),

    modBanEvent(2766, ModBanEvent.class),

    modKickEvent(2582, ModKickEvent.class),

    modMessageEvent(1840, ModMessageEvent.class),

    modMuteEvent(1945, ModMuteEvent.class),

    modToolRequestRoomUserChatlogEvent(-1, ModToolRequestRoomUserChatlogEvent.class),

    modToolSanctionEvent(1392, ModToolSanctionEvent.class),

    modToolWarnEvent(-1, ModToolWarnEvent.class),

    modTradingLockEvent(3742, ModTradingLockEvent.class),

    moderateMessageEvent(286, ModerateMessageEvent.class),

    moderateRoomEvent(3260, ModerateRoomEvent.class),

    moderateThreadEvent(1397, ModerateThreadEvent.class),

    moderatorActionEvent(3842, ModeratorActionEvent.class),

    mountPetEvent(1036, MountPetEvent.class),

    moveAvatarEvent(3320, MoveAvatarEvent.class),

    moveObjectEvent(248, MoveObjectEvent.class),

    movePetEvent(3449, MovePetEvent.class),

    moveWallItemEvent(168, MoveWallItemEvent.class),

    muteAllInRoomEvent(3637, MuteAllInRoomEvent.class),

    myFavouriteRoomsSearchEvent(2578, MyFavouriteRoomsSearchEvent.class),

    myFriendsRoomsSearchEvent(2266, MyFriendsRoomsSearchEvent.class),

    myGuildBasesSearchEvent(39, MyGuildBasesSearchEvent.class),

    myRoomHistorySearchEvent(2264, MyRoomHistorySearchEvent.class),

    myRoomRightsSearchEvent(272, MyRoomRightsSearchEvent.class),

    myRoomsSearchEvent(2277, MyRoomsSearchEvent.class),

    navigatorAddCollapsedCategoryEvent(1834, NavigatorAddCollapsedCategoryEvent.class),

    navigatorAddSavedSearchEvent(2226, NavigatorAddSavedSearchEvent.class),

    navigatorDeleteSavedSearchEvent(1954, NavigatorDeleteSavedSearchEvent.class),

    navigatorRemoveCollapsedCategoryEvent(637, NavigatorRemoveCollapsedCategoryEvent.class),

    navigatorSetSearchCodeViewModeEvent(1202, NavigatorSetSearchCodeViewModeEvent.class),

    newNavigatorInitEvent(2110, NewNavigatorInitEvent.class),

    newNavigatorSearchEvent(249, NewNavigatorSearchEvent.class),

    newUserExperienceGetGiftsEvent(1822, NewUserExperienceGetGiftsEvent.class),

    newUserExperienceScriptProceedEvent(1299, NewUserExperienceScriptProceedEvent.class),

    openCampaignCalendarDoorEvent(8809, OpenCampaignCalendarDoorEvent.class),

    openCampaignCalendarDoorAsStaffEvent(2507, OpenCampaignCalendarDoorAsStaffEvent.class),

    openFlatConnectionEvent(2312, OpenFlatConnectionEvent.class),

    openPetPackageEvent(3698, OpenPetPackageEvent.class),

    openTradingEvent(1481, OpenTradingEvent.class),

    passCarryItemEvent(2941, PassCarryItemEvent.class),

    pickIssuesEvent(15, PickIssuesEvent.class),

    pickupObjectEvent(3456, PickupObjectEvent.class),

    placeBotEvent(1592, PlaceBotEvent.class),

    placeObjectEvent(1258, PlaceObjectEvent.class),

    placePetEvent(2647, PlacePetEvent.class),

    placePostItEvent(2248, PlacePostItEvent.class),

    pollRejectEvent(1773, PollRejectEvent.class),

    pollStartEvent(109, PollStartEvent.class),

    pongEvent(2596, UnknownEvent2.class),

    popularRoomsSearchEvent(2758, PopularRoomsSearchEvent.class),

    postMessageEvent(3529, PostMessageEvent.class),

    presentOpenEvent(3558, PresentOpenEvent.class),

    publishPhotoEvent(2068, PublishPhotoEvent.class),

    purchaseFromCatalogAsGiftEvent(1411, PurchaseFromCatalogAsGiftEvent.class),

    purchaseFromCatalogEvent(3492, PurchaseFromCatalogEvent.class),

    purchasePhotoEvent(2408, PurchasePhotoEvent.class),

    purchaseRoomAdEvent(777, PurchaseRoomAdEvent.class),

    purchaseTargetedOfferEvent(1826, PurchaseTargetedOfferEvent.class),

    purchaseVipMembershipExtensionEvent(3407, PurchaseVipMembershipExtensionEvent.class),

    quitEvent(105, QuitEvent.class),

    rateFlatEvent(3582, RateFlatEvent.class),

    recycleItemsEvent(2771, RecycleItemsEvent.class),

    redeemMarketplaceOfferCreditsEvent(2650, RedeemMarketplaceOfferCreditsEvent.class),

    redeemVoucherEvent(339, RedeemVoucherEvent.class),

    rejectMembershipRequestEvent(1894, RejectMembershipRequestEvent.class),

    releaseIssuesEvent(1572, ReleaseIssuesEvent.class),

    removeAdminRightsFromMemberEvent(722, RemoveAdminRightsFromMemberEvent.class),

    removeAllRightsEvent(2683, RemoveAllRightsEvent.class),

    removeBotFromFlatEvent(3323, RemoveBotFromFlatEvent.class),

    removeFriendEvent(1689, RemoveFriendEvent.class),

    removeItemEvent(3336, RemoveItemEvent.class),

    removeItemFromTradeEvent(3845, RemoveItemFromTradeEvent.class),

    removeJukeboxDiskEvent(3050, RemoveJukeboxDiskEvent.class),

    removeOwnRoomRightsRoomEvent(3182, RemoveOwnRoomRightsRoomEvent.class),

    removePetFromFlatEvent(1581, RemovePetFromFlatEvent.class),

    removeRightsEvent(2064, RemoveRightsEvent.class),

    removeSaddleFromPetEvent(186, RemoveSaddleFromPetEvent.class),

    renderRoomEvent(3226, RenderRoomEvent.class),

    renderRoomThumbnailEvent(1982, RenderRoomThumbnailEvent.class),

    rentableSpaceCancelRentEvent(1667, RentableSpaceCancelRentEvent.class),

    rentableSpaceRentEvent(2946, RentableSpaceRentEvent.class),

    requestAchievementConfigurationEvent(-1, RequestAchievementConfigurationEvent.class),

    requestCameraConfigurationEvent(796, RequestCameraConfigurationEvent.class),

    requestFriendEvent(3157, RequestFriendEvent.class),

    requestFurniInventoryEvent(3150, RequestFurniInventoryWhenNotInRoomEvent.class),

    requestFurniInventoryWhenNotInRoomEvent(3500, RequestFurniInventoryWhenNotInRoomEvent.class),

    requestRoomPropertySet(711, RequestRoomPropertySet.class),

    respectPetEvent(3202, RespectPetEvent.class),

    respectUserEvent(2694, RespectUserEvent.class),

    roomDimmerChangeStateEvent(2296, RoomDimmerChangeStateEvent.class),

    roomDimmerGetPresetsEvent(2813, RoomDimmerGetPresetsEvent.class),

    roomDimmerSavePresetEvent(1648, RoomDimmerSavePresetEvent.class),

    roomTextSearchEvent(3943, RoomTextSearchEvent.class),

    roomUserKickEvent(1320, RoomUserKickEvent.class),

    roomUserMuteEvent(3485, RoomUserMuteEvent.class),

    roomsWhereMyFriendsAreSearchEvent(1786, RoomsWhereMyFriendsAreSearchEvent.class),

    roomsWithHighestScoreSearchEvent(2939, RoomsWithHighestScoreSearchEvent.class),

    sSOTicketEvent(2419, SSOTicketEvent.class),

    saveRoomSettingsEvent(1969, SaveRoomSettingsEvent.class),

    saveWardrobeOutfitEvent(800, SaveWardrobeOutfitEvent.class),

    scrGetKickbackInfoEvent(869, ScrGetKickbackInfoEvent.class),

    scrGetUserInfoEvent(3166, ScrGetUserInfoEvent.class),

    searchRoomsByTagEvent(-1, SearchRoomsByTagEvent.class),

    selectClubGiftEvent(2276, SelectClubGiftEvent.class),

    selectFavouriteHabboGroupEvent(3549, SelectFavouriteHabboGroupEvent.class),

    sendMsgEvent(3567, SendMsgEvent.class),

    sendRoomInviteEvent(1276, SendRoomInviteEvent.class),

    setActivatedBadgesEvent(644, SetActivatedBadgesEvent.class),

    setChatPreferencesEvent(1262, SetChatPreferencesEvent.class),

    setChatStylePreferenceEvent(1030, SetChatStylePreferenceEvent.class),

    setClothingChangeDataEvent(924, SetClothingChangeDataEvent.class),

    setCustomStackingHeightEvent(3839, SetCustomStackingHeightEvent.class),

    setIgnoreRoomInvitesEvent(1086, SetIgnoreRoomInvitesEvent.class),

    setItemDataEvent(3666, SetItemDataEvent.class),

    setMannequinFigureEvent(2209, SetMannequinFigureEvent.class),

    setMannequinNameEvent(2850, SetMannequinNameEvent.class),

    setNewNavigatorWindowPreferencesEvent(3159, SetNewNavigatorWindowPreferencesEvent.class),

    setObjectDataEvent(3608, SetObjectDataEvent.class),

    setRandomStateEvent(3617, SetRandomStateEvent.class),

    setRelationshipStatusEvent(3768, SetRelationshipStatusEvent.class),

    setRoomBackgroundColorDataEvent(2880, SetRoomBackgroundColorDataEvent.class),

    setRoomCameraPreferencesEvent(1461, SetRoomCameraPreferencesEvent.class),

    setSoundSettingsEvent(1367, SetSoundSettingsEvent.class),

    setTargetedOfferStateEvent(2041, SetTargetedOfferStateEvent.class),

    setYoutubeDisplayPlaylistEvent(2069, SetYoutubeDisplayPlaylistEvent.class),

    shoutEvent(2085, ShoutEvent.class),

    signEvent(1975, SignEvent.class),

    spinWheelOfFortuneEvent(2144, SpinWheelOfFortuneEvent.class),

    startTypingEvent(1597, StartTypingEvent.class),

    submitRoomToCompetitionEvent(2595, SubmitRoomToCompetitionEvent.class),

    throwDiceEvent(1990, ThrowDiceEvent.class),

    togglePetBreedingPermissionEvent(3379, TogglePetBreedingPermissionEvent.class),

    togglePetRidingPermissionEvent(1472, TogglePetRidingPermissionEvent.class),

    toggleStaffPickEvent(1918, ToggleStaffPickEvent.class),

    unacceptTradingEvent(1444, UnacceptTradingEvent.class),

    unbanUserFromRoomEvent(992, UnbanUserFromRoomEvent.class),

    unignoreUserEvent(2061, UnignoreUserEvent.class),

    uniqueIDEvent(2490, UniqueIDEvent.class),

    updateActionEvent(2281, UpdateActionEvent.class),

    updateConditionEvent(3203, UpdateConditionEvent.class),

    updateFigureDataEvent(2730, UpdateFigureDataEvent.class),

    updateFloorPropertiesEvent(875, UpdateFloorPropertiesEvent.class),

    updateForumSettingsEvent(2214, UpdateForumSettingsEvent.class),

    updateGuildBadgeEvent(1991, UpdateGuildBadgeEvent.class),

    updateGuildColorsEvent(1764, UpdateGuildColorsEvent.class),

    updateGuildIdentityEvent(3137, UpdateGuildIdentityEvent.class),

    updateGuildSettingsEvent(3435, UpdateGuildSettingsEvent.class),

    updateHomeRoomEvent(1740, UpdateHomeRoomEvent.class),

    updateRoomFilterEvent(3001, UpdateRoomFilterEvent.class),

    updateThreadEvent(3045, UpdateThreadEvent.class),

    updateTriggerEvent(1520, UpdateTriggerEvent.class),

    updateUIFlagsEvent(2313, UpdateUIFlagsEvent.class),

    useFurnitureEvent(99, UseFurnitureEvent.class),

    useWallItemEvent(210, UseWallItemEvent.class),

    visitUserEvent(3997, VisitUserEvent.class),

    whisperEvent(1543, WhisperEvent.class),

    acceptGameInviteEvent(3802, UnknownEvent2.class),

    acceptQuestEvent(3604, UnknownEvent2.class),

    approveAllMembershipRequestsEvent(882, UnknownEvent2.class),

    buildersClubPlaceRoomItemEvent(1051, UnknownEvent2.class),

    buyMarketplaceTokensEvent(1866, UnknownEvent2.class),

    callForHelpFromSelfieEvent(2755, UnknownEvent2.class),

    cancelEventEvent(2725, UnknownEvent2.class),

    cancelQuestEvent(3133, UnknownEvent2.class),

    changeEmailEvent(3965, UnknownEvent2.class),

    changeQueueEvent(3093, UnknownEvent2.class),

    communityGoalVoteEvent(3536, UnknownEvent2.class),

    competitionRoomsSearchEvent(433, UnknownEvent2.class),

    deletePendingCallsForHelpEvent(3605, UnknownEvent2.class),

    disconnectEvent(2445, UnknownEvent2.class),

    extendRentOrBuyoutFurniEvent(1071, UnknownEvent2.class),

    extendRentOrBuyoutStripItemEvent(2115, UnknownEvent2.class),

    forwardToACompetitionRoomEvent(172, UnknownEvent2.class),

    forwardToARandomPromotedRoomEvent(10, UnknownEvent2.class),

    forwardToRandomCompetitionRoomEvent(865, UnknownEvent2.class),

    friendListUpdateEvent(1419, UnknownEvent2.class),

    friendRequestQuestCompleteEvent(1148, UnknownEvent2.class),

    game2CheckGameDirectoryStatusEvent(3259, UnknownEvent2.class),

    game2ExitGameEvent(1445, UnknownEvent2.class),

    game2GameChatEvent(2502, UnknownEvent2.class),

    game2GetWeeklyFriendsLeaderboardEvent(1232, UnknownEvent2.class),

    game2GetWeeklyLeaderboardEvent(2565, UnknownEvent2.class),

    game2LoadStageReadyEvent(2415, UnknownEvent2.class),

    game2PlayAgainEvent(3196, UnknownEvent2.class),

    game2RequestFullStatusUpdateEvent(1598, UnknownEvent2.class),

    getCatalogPageExpirationEvent(742, UnknownEvent2.class),

    getCatalogPageWithEarliestExpiryEvent(3135, UnknownEvent2.class),

    getCategoriesWithUserCountEvent(3782, UnknownEvent2.class),

    getCommunityGoalEarnedPrizesEvent(2688, UnknownEvent2.class),

    getCommunityGoalHallOfFameEvent(2167, UnknownEvent2.class),

    getCommunityGoalProgressEvent(1145, UnknownEvent2.class),

    getConcurrentUsersGoalProgressEvent(1343, UnknownEvent2.class),

    getConcurrentUsersRewardEvent(3872, UnknownEvent2.class),

    getDailyQuestEvent(2486, UnknownEvent2.class),

    getDirectClubBuyAvailableEvent(801, UnknownEvent2.class),

    getEmailStatusEvent(2557, UnknownEvent2.class),

    getExtendedProfileByNameEvent(2249, UnknownEvent2.class),

    getFaqCategoryEvent(3445, UnknownEvent2.class),

    getFaqTextEvent(1849, UnknownEvent2.class),

    getGameAchievementsEvent(2399, UnknownEvent2.class),

    getHabboBasicMembershipExtendOfferEvent(603, UnknownEvent2.class),

    getInterstitialEvent(2519, UnknownEvent2.class),

    getIsBadgeRequestFulfilledEvent(1364, UnknownEvent2.class),

    getIsOfferGiftableEvent(1347, UnknownEvent2.class),

    getIsUserPartOfCompetitionEvent(2077, UnknownEvent2.class),

    getNextTargetedOfferEvent(2487, UnknownEvent2.class),

    getOfficialRoomsEvent(1229, UnknownEvent2.class),

    getQuestsEvent(3333, UnknownEvent2.class),

    getQuizQuestionsEvent(1296, UnknownEvent2.class),

    getSeasonalCalendarDailyOfferEvent(3257, UnknownEvent2.class),

    getSeasonalQuestsOnlyEvent(1190, UnknownEvent2.class),

    getSoundMachinePlayListEvent(3498, UnknownEvent2.class),

    getTargetedOfferEvent(596, UnknownEvent2.class),

    getThreadEvent(3900, UnknownEvent2.class),

    getUserGameAchievementsEvent(389, UnknownEvent2.class),

    giveSupplementToPetEvent(749, UnknownEvent2.class),

    goToFlatEvent(685, UnknownEvent2.class),

    guideAdvertisementReadEvent(2455, UnknownEvent2.class),

    guildBaseSearchEvent(2930, UnknownEvent2.class),

    harvestPetEvent(1521, UnknownEvent2.class),

    ignoreUserIdEvent(3314, UnknownEvent2.class),

    interstitialShownEvent(1109, UnknownEvent2.class),

    lagWarningReportEvent(3847, UnknownEvent2.class),

    latencyPingReportEvent(96, UnknownEvent2.class),

    leaveQueueEvent(2384, UnknownEvent2.class),

    modToolPreferencesEvent(31, UnknownEvent2.class),

    myFrequentRoomHistorySearchEvent(1002, UnknownEvent2.class),

    myRecommendedRoomsEvent(2537, UnknownEvent2.class),

    mysteryBoxWaitingCanceledEvent(2012, UnknownEvent2.class),

    openMysteryTrophyEvent(3074, UnknownEvent2.class),

    openQuestTrackerEvent(2750, UnknownEvent2.class),

    openWelcomeGiftEvent(2638, UnknownEvent2.class),

    passCarryItemToPetEvent(2768, UnknownEvent2.class),

    peerUsersClassificationEvent(1160, UnknownEvent2.class),

    performanceLogEvent(3230, UnknownEvent2.class),

    petSelectedEvent(549, UnknownEvent2.class),

    photoCompetitionEvent(3959, UnknownEvent2.class),

    postQuizAnswersEvent(3720, UnknownEvent2.class),

    purchaseBasicMembershipExtensionEvent(2735, UnknownEvent2.class),

    redeemCommunityGoalPrizeEvent(90, UnknownEvent2.class),

    rejectQuestEvent(2397, UnknownEvent2.class),

    rentableSpaceStatusEvent(872, UnknownEvent2.class),

    requestABadgeEvent(3077, UnknownEvent2.class),

    resetPhoneNumberStateEvent(2741, UnknownEvent2.class),

    resetResolutionAchievementEvent(3144, UnknownEvent2.class),

    resetUnseenItemIdsEvent(3493, UnknownEvent2.class),

    resetUnseenItemsEvent(2343, UnknownEvent2.class),

    roomAdEventTabAdClickedEvent(2412, UnknownEvent2.class),

    roomAdEventTabViewedEvent(2668, UnknownEvent2.class),

    roomAdPurchaseInitiatedEvent(2283, UnknownEvent2.class),

    roomAdSearchEvent(2809, UnknownEvent2.class),

    roomCompetitionInitEvent(1334, UnknownEvent2.class),

    roomNetworkOpenConnectionEvent(3736, UnknownEvent2.class),

    roomUsersClassificationEvent(2285, UnknownEvent2.class),

    searchFaqsEvent(2031, UnknownEvent2.class),

    setPhoneNumberVerificationStatusEvent(1379, UnknownEvent2.class),

    setRoomSessionTagsEvent(3305, UnknownEvent2.class),

    shopTargetedOfferViewedEvent(3483, UnknownEvent2.class),

    startCampaignEvent(1697, UnknownEvent2.class),

    tryPhoneNumberEvent(790, UnknownEvent2.class),

    unblockGroupMemberEvent(2864, UnknownEvent2.class),

    updateForumReadMarkerEvent(1855, UnknownEvent2.class),

    updateRoomCategoryAndTradeSettingsEvent(1265, UnknownEvent2.class),

    updateRoomThumbnailEvent(2468, UnknownEvent2.class),

    verifyCodeEvent(2721, UnknownEvent2.class),

    versionCheckEvent(1053, UnknownEvent2.class),

    voteForRoomEvent(143, UnknownEvent2.class),

    welcomeGiftChangeEmailEvent(66, UnknownEvent2.class),

    // Roleplay
    attackUserEvent(8001, AttackUserEvent.class),

    corpAcceptJobOfferEvent(8002, CorpAcceptJobEvent.class),

    corpDeclineJobOfferEvent(8003, CorpDeclineOfferEvent.class),

    corpDemoteUserEvent(8004, CorpDemoteUserEvent.class),

    corpFireUserEvent(8005, CorpFireUserEvent.class),

    corpAcceptJobEvent(8006, CorpOfferUserJobEvent.class),

    corpPromoteUserEvent(8006, CorpPromoteUserEvent.class),

    corpStartWorkEvent(8011, CorpStartWorkEvent.class),

    corpStopWorkEvent(8012, CorpStopWorkEvent.class),

    gangDeclineInviteEvent(8008, GangDeclineInviteEvent.class),

    gangDisbandEvent(8013, GangDisbandEvent.class),

    gangInviteAcceptEvent(8009, GangInviteAcceptEvent.class),

    gangInviteUserEvent(8010, GangInviteUserEvent.class),

    gangLeaveEvent(8014, GangLeaveEvent.class),

    userRoleplayStatsQueryEvent(8015, UserRoleplayStatsQueryComposer.class),

    corpInfoQueryEvent(8016, CorpInfoQueryEvent.class),

    gangInfoQueryEvent(8017, GangInfoQueryEvent.class),

    corpPositionInfoQueryEvent(8018, CorpPositionInfoQueryEvent.class),

    timeOfDayQueryEvent(8019, TimeOfDayQueryEvent.class),

    policeArrestEvent(8020, PoliceArrestEvent.class),

    policeCuffEvent(8021, PoliceCuffEvent.class),

    policeStunEvent(8022, PoliceStunEvent.class),

    policeEscortEvent(8024, PoliceEscortEvent.class),

    equipWeaponEvent(8025, EquipWeaponEvent.class),

    listMyWeaponsEvent(8026, ListMyWeaponsEvent.class);


    private final int messageId;
    private final Class<? extends MessageHandler> messageClass;
}