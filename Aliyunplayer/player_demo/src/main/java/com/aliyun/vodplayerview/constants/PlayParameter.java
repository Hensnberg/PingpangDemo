package com.aliyun.vodplayerview.constants;

/**
 * 播放参数, 包含:
 * vid, vidSts, akId, akSecre, scuToken
 */
public class PlayParameter {


    /**
     * type, 用于区分播放类型, 默认为vidsts播放
     * vidsts: vid类型
     * localSource: url类型
     */
    public static String PLAY_PARAM_TYPE = "vidAuth";

    /**
     * vid初始值
     */
    public static final String PLAY_PARAM_VID_DEFAULT = "9caa942ee6fe489ca8ac4ff01ae73dd3";

    /**
     * vid
     */
    public static String PLAY_PARAM_VID = "c23cac4f169648f2b77fd587478ad091";

    public static String PLAY_PARAM_REGION = "cn-shanghai";

    /**
     * url类型的播放地址, 初始为:http://player.alicdn.com/video/aliyunmedia.mp4
     */
    public static String PLAY_PARAM_URL = "https://outin-8649ec7f276e11ecaa6300163e00b174.oss-cn-shanghai.aliyuncs.com/sv/355ec15d-1845764c25c/355ec15d-1845764c25c.mp4?Expires=1669091040&OSSAccessKeyId=LTAI8bKSZ6dKjf44&Signature=OjDCPqVVNBBHHC%2Fw%2FOJDS7kzyi0%3D";


    /**
     * 播放凭证VidAuth
     */
    public static String PLAY_PARAM_TYPE_AUTH_PLAY_AUTH = "playAuth";

    /**
     * 标题
     */
    public static String PLAY_PARAM_TYPE_AUTH_TITLE = "title";

    /**
     * 封面
     */
    public static String PLAY_PARAM_TYPE_AUTH_COVER_PATH = "coverPath";

}
