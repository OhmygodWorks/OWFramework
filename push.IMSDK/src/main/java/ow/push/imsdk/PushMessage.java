package ow.push.imsdk;

import android.os.Parcel;
import android.os.Parcelable;

import static java.lang.System.currentTimeMillis;

/**
 * IMSDK专用的推送消息格式。
 *
 * Created by ohmygod on 17/4/4.
 */
@SuppressWarnings("WeakerAccess")
public class PushMessage implements ow.PushMessage, Parcelable {
    public final int fromUserID;
    public final long timestamp;
    private final String messageID, rawContent;

    protected PushMessage(String messageID, int fromUserID, String rawContent) {
        this.messageID = messageID;
        this.fromUserID = fromUserID;
        this.rawContent = rawContent;
        this.timestamp = currentTimeMillis();
    }

    /**
     * @return 收到的原始消息内容
     */
    @Override
    public String getRawMessageContent() {
        return rawContent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PushMessage that = (PushMessage) o;

        return messageID != null ? messageID.equals(that.messageID) : that.messageID == null;

    }

    @Override
    public int hashCode() {
        return messageID != null ? messageID.hashCode() : 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.fromUserID);
        dest.writeLong(this.timestamp);
        dest.writeString(this.messageID);
        dest.writeString(this.rawContent);
    }

    protected PushMessage(Parcel in) {
        this.fromUserID = in.readInt();
        this.timestamp = in.readLong();
        this.messageID = in.readString();
        this.rawContent = in.readString();
    }

    public static final Parcelable.Creator<PushMessage> CREATOR = new Parcelable.Creator<PushMessage>() {
        @Override
        public PushMessage createFromParcel(Parcel source) {
            return new PushMessage(source);
        }

        @Override
        public PushMessage[] newArray(int size) {
            return new PushMessage[size];
        }
    };
}
