package cn.wildfire.chat.kit.contact;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cn.wildfire.chat.kit.common.AppScopeViewModel;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.FriendRequest;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.OnFriendUpdateListener;
import cn.wildfirechat.remote.SearchUserCallback;

public class ContactViewModel extends ViewModel implements OnFriendUpdateListener, AppScopeViewModel {
    private MutableLiveData<List<UserInfo>> contactListLiveData;
    private MutableLiveData<Integer> friendRequestUpdatedLiveData;

    public ContactViewModel() {
        super();
        ChatManager.Instance().addFriendUpdateListener(this);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ChatManager.Instance().removeFriendUpdateListener(this);
    }

    public MutableLiveData<List<UserInfo>> contactListLiveData() {
        if (contactListLiveData == null) {
            contactListLiveData = new MutableLiveData<>();
        }
        reloadContact();
        return contactListLiveData;
    }

    public MutableLiveData<Integer> friendRequestUpdatedLiveData() {
        if (friendRequestUpdatedLiveData == null) {
            friendRequestUpdatedLiveData = new MutableLiveData<>();
        }
        int count = getUnreadFriendRequestCount();
        if (count > 0) {
            friendRequestUpdatedLiveData.setValue(count);
        }
        return friendRequestUpdatedLiveData;
    }

    public List<String> getFriends(boolean refresh) {
        return ChatManager.Instance().getMyFriendList(refresh);
    }

    private AtomicInteger loadingCount = new AtomicInteger(0);

    public void reloadContact() {
        int count = loadingCount.get();
        if (count > 0) {
            return;
        }
        loadingCount.incrementAndGet();
        ChatManager.Instance().getWorkHandler().post(() -> {
            loadingCount.decrementAndGet();
            List<UserInfo> userInfos = ChatManager.Instance().getMyFriendListInfo(false);
            if (contactListLiveData != null) {
                contactListLiveData.postValue(userInfos);
            }
        });
    }

    public List<UserInfo> getContacts(boolean refresh) {
        return ChatManager.Instance().getMyFriendListInfo(refresh);
    }

    public int getUnreadFriendRequestCount() {
        return ChatManager.Instance().getUnreadFriendRequestStatus();
    }

    public void clearUnreadFriendRequestStatus() {
        ChatManager.Instance().clearUnreadFriendRequestStatus();
    }

    @Override
    public void onFriendListUpdate(List<String> updateFriendList) {
        reloadContact();
    }

    @Override
    public void onFriendRequestUpdate() {
        if (friendRequestUpdatedLiveData != null) {
            friendRequestUpdatedLiveData.setValue(getUnreadFriendRequestCount());
        }
    }

    public List<FriendRequest> getFriendRequest() {
        return ChatManager.Instance().getFriendRequest(true);
    }

    public MutableLiveData<Boolean> acceptFriendRequest(String friendId) {

        MutableLiveData<Boolean> result = new MutableLiveData<>();
        ChatManager.Instance().handleFriendRequest(friendId, true, new GeneralCallback() {
            @Override
            public void onSuccess() {
                ChatManager.Instance().loadFriendRequestFromRemote();
                List<FriendRequest> inComingFriendRequests = ChatManager.Instance().getFriendRequest(true);
                for (FriendRequest request : inComingFriendRequests) {
                    if (request.target.equals(friendId)) {
                        result.setValue(true);
                        return;
                    }
                }
                result.setValue(false);
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(false);
            }
        });
        return result;
    }

    public MutableLiveData<List<UserInfo>> searchUser(String keyword, boolean fuzzy) {
        MutableLiveData<List<UserInfo>> result = new MutableLiveData<>();
        ChatManager.Instance().searchUser(keyword, fuzzy, new SearchUserCallback() {
            @Override
            public void onSuccess(List<UserInfo> userInfos) {
                result.setValue(userInfos);
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(null);
            }
        });

        return result;
    }

    public boolean isFriend(String targetUid) {
        return ChatManager.Instance().isMyFriend(targetUid);
    }

    public LiveData<OperateResult<Boolean>> deleteFriend(String userId) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().deleteFriend(userId, new GeneralCallback() {
            @Override
            public void onSuccess() {
                ChatManager.Instance().removeConversation(new Conversation(Conversation.ConversationType.Single, userId, 0), true);
                result.postValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.postValue(new OperateResult<>(errorCode));
            }
        });

        return result;
    }

    public MutableLiveData<Boolean> invite(String targetUid, String message) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        ChatManager.Instance().sendFriendRequest(targetUid, message, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(true);
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(false);
            }
        });
        return result;
    }

}
