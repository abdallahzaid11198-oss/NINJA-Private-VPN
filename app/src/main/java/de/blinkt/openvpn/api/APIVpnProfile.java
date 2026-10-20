package de.blinkt.openvpn.api;
import android.os.Parcel;
import android.os.Parcelable;
/** Compatible parcelable for the external OpenVPN Android API. */
public final class APIVpnProfile implements Parcelable {
  public final String mUUID, mName;
  public final boolean mUserEditable;
  public APIVpnProfile(Parcel p) {
    mUUID = p.readString(); mName = p.readString();
    mUserEditable = p.readInt() != 0;
  }
  public APIVpnProfile(String id, String name, boolean editable) {
    mUUID=id; mName=name; mUserEditable=editable;
  }
  public void writeToParcel(Parcel p, int flags) {
    p.writeString(mUUID); p.writeString(mName); p.writeInt(mUserEditable ? 1 : 0);
  }
  public int describeContents() { return 0; }
  public static final Creator<APIVpnProfile> CREATOR=new Creator<APIVpnProfile>() {
    public APIVpnProfile createFromParcel(Parcel p) { return new APIVpnProfile(p); }
    public APIVpnProfile[] newArray(int n) { return new APIVpnProfile[n]; }
  };
}
