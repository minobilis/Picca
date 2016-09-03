/*
 * This is the source code of Telegram for Android v. 2.0.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Author Nikolai Kudashov.
 */

package com.roshin.gallery.query;

import com.roshin.gallery.AndroidUtilities;
import com.roshin.gallery.ImageLoader;
import com.roshin.gallery.NotificationCenter;
import com.roshin.tgnet.ConnectionsManager;
import com.roshin.tgnet.RequestDelegate;
import com.roshin.tgnet.TLObject;
import com.roshin.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;

public class SharedMediaQuery {

    public final static int MEDIA_PHOTOVIDEO = 0;
    public final static int MEDIA_FILE = 1;
    public final static int MEDIA_AUDIO = 2;
    public final static int MEDIA_URL = 3;
    public final static int MEDIA_MUSIC = 4;
    public final static int MEDIA_TYPES_COUNT = 5;

    public static void loadMedia(final long uid, final int offset, final int count, final int max_id, final int type, final boolean fromCache, final int classGuid) {
        final boolean isChannel = uid < 0;

        int lower_part = (int)uid;
        if (fromCache || lower_part == 0) {
            loadMediaDatabase(uid, offset, count, max_id, type, classGuid, isChannel);
        } else {
            TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
            req.offset = offset;
            req.limit = count + 1;
            req.max_id = max_id;
            if (type == MEDIA_PHOTOVIDEO) {
                req.filter = new TLRPC.TL_inputMessagesFilterPhotoVideo();
            } else if (type == MEDIA_FILE) {
                req.filter = new TLRPC.TL_inputMessagesFilterDocument();
            } else if (type == MEDIA_AUDIO) {
                req.filter = new TLRPC.TL_inputMessagesFilterVoice();
            } else if (type == MEDIA_URL) {
                req.filter = new TLRPC.TL_inputMessagesFilterUrl();
            } else if (type == MEDIA_MUSIC) {
                req.filter = new TLRPC.TL_inputMessagesFilterMusic();
            }
            req.q = "";
            req.peer = null;
            if (req.peer == null) {
                return;
            }
            int reqId = ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                @Override
                public void run(TLObject response, TLRPC.TL_error error) {
                    if (error == null) {
                        final TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                        boolean topReached;
                        if (res.messages.size() > count) {
                            topReached = false;
                            res.messages.remove(res.messages.size() - 1);
                        } else {
                            topReached = true;
                        }
                        processLoadedMedia(res, uid, offset, count, max_id, type, false, classGuid, isChannel, topReached);
                    }
                }
            });
            ConnectionsManager.getInstance().bindRequestToGuid(reqId, classGuid);
        }
    }

    public static void getMediaCount(final long uid, final int type, final int classGuid, boolean fromCache) {
        int lower_part = (int)uid;
        if (fromCache || lower_part == 0) {
            getMediaCountDatabase(uid, type, classGuid);
        } else {
            TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
            req.offset = 0;
            req.limit = 1;
            req.max_id = 0;
            if (type == MEDIA_PHOTOVIDEO) {
                req.filter = new TLRPC.TL_inputMessagesFilterPhotoVideo();
            } else if (type == MEDIA_FILE) {
                req.filter = new TLRPC.TL_inputMessagesFilterDocument();
            } else if (type == MEDIA_AUDIO) {
                req.filter = new TLRPC.TL_inputMessagesFilterVoice();
            } else if (type == MEDIA_URL) {
                req.filter = new TLRPC.TL_inputMessagesFilterUrl();
            } else if (type == MEDIA_MUSIC) {
                req.filter = new TLRPC.TL_inputMessagesFilterMusic();
            }
            req.q = "";
            req.peer = null;
            if (req.peer == null) {
                return;
            }
            int reqId = ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                @Override
                public void run(TLObject response, TLRPC.TL_error error) {
                    if (error == null) {
                        final TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                        int count;
                        if (res instanceof TLRPC.TL_messages_messages) {
                            count = res.messages.size();
                        } else {
                            count = res.count;
                        }

                        processLoadedMediaCount(count, uid, type, classGuid, false);
                    }
                }
            });
            ConnectionsManager.getInstance().bindRequestToGuid(reqId, classGuid);
        }
    }

    private static void processLoadedMedia(final TLRPC.messages_Messages res, final long uid, int offset, int count, int max_id, final int type, final boolean fromCache, final int classGuid, final boolean isChannel, final boolean topReached) {
        int lower_part = (int)uid;
        if (fromCache && res.messages.isEmpty() && lower_part != 0) {
            loadMedia(uid, offset, count, max_id, type, false, classGuid);
        } else {
            if (!fromCache) {
                ImageLoader.saveMessagesThumbs(res.messages);
                putMediaDatabase(uid, type, res.messages, max_id, topReached);
            }

            final HashMap<Integer, TLRPC.User> usersDict = new HashMap<>();
            for (int a = 0; a < res.users.size(); a++) {
                TLRPC.User u = res.users.get(a);
                usersDict.put(u.id, u);
            }
            final ArrayList<Object> objects = new ArrayList<>();

            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    int totalCount = res.count;
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.mediaDidLoaded, uid, totalCount, objects, classGuid, type, topReached);
                }
            });
        }
    }

    private static void processLoadedMediaCount(final int count, final long uid, final int type, final int classGuid, final boolean fromCache) {
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                int lower_part = (int) uid;
                if (fromCache && count == -1 && lower_part != 0) {
                    getMediaCount(uid, type, classGuid, false);
                } else {
                    if (!fromCache) {
                        putMediaCountDatabase(uid, type, count);
                    }
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.mediaCountDidLoaded, uid, (fromCache && count == -1 ? 0 : count), fromCache, type);
                }
            }
        });
    }

    private static void putMediaCountDatabase(final long uid, final int type, final int count) {

    }

    private static void getMediaCountDatabase(final long uid, final int type, final int classGuid) {

    }

    private static void loadMediaDatabase(final long uid, final int offset, final int count, final int max_id, final int type, final int classGuid, final boolean isChannel) {

    }

    private static void putMediaDatabase(final long uid, final int type, final ArrayList<TLRPC.Message> messages, final int max_id, final boolean topReached) {

    }
}
