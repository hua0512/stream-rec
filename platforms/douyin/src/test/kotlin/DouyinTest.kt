import github.hua0512.app.App
import github.hua0512.data.config.AppConfig
import github.hua0512.plugins.douyin.download.Douyin
import github.hua0512.plugins.douyin.download.DouyinExtractor
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.DefaultAsserter.assertEquals
import kotlin.test.DefaultAsserter.assertNotNull
import kotlin.test.Test

/*
 * MIT License
 *
 * Stream-rec  https://github.com/hua0512/stream-rec
 *
 * Copyright (c) 2024 hua0512 (https://github.com/hua0512)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

class DouyinTest {


  @Test
  fun testUrl() = runTest {
    val url = "https://live.douyin.com/217536353956?123112321321"
    val matchResult = DouyinExtractor.URL_REGEX.toRegex().find(url) ?: throw IllegalArgumentException("Invalid url")
    assertEquals("failed to match id", matchResult.groupValues.last(), "217536353956")
  }

  @Test
  fun testAvatarRegex() = runTest {
    val content = """
      data: {"data":{"data":[{"id_str":"7340649709855410956","status":4,"status_str":"4","title":"不是在战斗的路上就是在睡觉的途中","user_count_str":"0","mosaic_status":0,"mosaic_status_str":"","admin_user_ids":[],"admin_user_ids_str":[],"live_room_mode":1,"has_commerce_goods":false,"linker_map":{},"AnchorABMap":{},"like_count":0,"owner_user_id_str":""}],"enter_room_id":"7340649709855410956","user":{"id_str":"976835324687115","sec_uid":"MS4wLjABAAAADzEWuDTRRKvujzMQAvc3BEBpfTsqwpPWqKKR4APK_Qo","nickname":"小羊崽（无畏契约）","avatar_thumb":{"url_list":["https://p26.douyinpic.com/aweme/100x100/aweme-avatar/tos-cn-avt-0015_91c6d02d076a3943c80db5664c9541bb.jpeg?from=3067671334","https://p3.douyinpic.com/aweme/100x100/aweme-avatar/tos-cn-avt-0015_91c6d02d076a3943c80db5664c9541bb.jpeg?from=3067671334","https://p11.douyinpic.com/aweme/100x100/aweme-avatar/tos-cn-avt-0015_91c6d02d076a3943c80db5664c9541bb.jpeg?from=3067671334"]},"follow_info":{"follow_status":0,"follow_status_str":"0"}},"qrcode_url":"","enter_mode":0,"room_status":2,"partition_road_map":{"partition":{"id_str":"1","type":1,"title":"射击游戏"},"sub_partition":{"partition":{"id_str":"1010017","type":1,"title":""}}},"similar_rooms":[{"room":{"id_str":"7340745057483442994","status":0,"status_str":"0","title":"LGD退役职业选手兼金牌解说单排顶分神话局帮粉丝上分","user_count_str":"1000+","cover":{"url_list":["https://p3-webcast-sign.douyinpic.com/webcast-cover/7328056075125607206~tplv-qz53dukwul-common-resize:360:0.image?lk3s=7945d550\u0026x-expires=1711755958\u0026x-signature=I9qpjMgmoij1%2FDqWcNx2Pzv6Kxs%3D","https://p6-webcast-sign.douyinpic.com/webcast-cover/7328056075125607206~tplv-qz53dukwul-common-resize:360:0.image?lk3s=7945d550\u0026x-expires=1711755958\u0026x-signature=dWFnHK1wy2d1ukbSBRTCxsRU7Aw%3D"]},"stream_url":{"flv_pull_url":{"FULL_HD1":"http://pull-flv-l26.douyincdn.com/third/stream-114699159197975014_uhd.flv?expire=65e90036\u0026sign=855437ae582cc8338a756542167aa26d","HD1":"http://pull-flv-l26.douyincdn.com/third/stream-114699159197975014_hd.flv?expire=65e90036\u0026sign=a37489d89fd67adee8b45f725d303495","SD1":"http://pull-flv-l26.douyincdn.com/third/stream-114699159197975014_ld.flv?expire=65e90036\u0026sign=efccd9b5b05039aa55b883f07612af8b","SD2":"http://pull-flv-l26.douyincdn.com/third/stream-114699159197975014_sd.flv?expire=65e90036\u0026sign=5da4bbcde0edba5dd27b11951c0a72cb"},"default_resolution":"HD1","hls_pull_url_map":{"FULL_HD1":"http://pull-hls-l26.douyincdn.com/third/stream-114699159197975014_uhd.m3u8?expire=65e90036\u0026sign=203f18accd970d3bab50650321905d82","HD1":"http://pull-hls-l26.douyincdn.com/third/stream-114699159197975014_hd.m3u8?expire=65e90036\u0026sign=cb2615212306aaf7e1adf03d7ed3bcd2","SD1":"http://pull-hls-l26.douyincdn.com/third/stream-114699159197975014_ld.m3u8?expire=65e90036\u0026sign=38b7513e46d7fe5a1823c133471d3c93","SD2":"http://pull-hls-l26.douyincdn.com/third/stream-114699159197975014_sd.m3u8?expire=65e90036\u0026sign=74dc48d61f34c411c9be2016565f67bb"},"hls_pull_url":"http://pull-hls-l26.douyincdn.com/third/stream-114699159197975014_hd.m3u8?expire=65e90036\u0026sign=cb2615212306aaf7e1adf03d7ed3bcd2","stream_orientation":2,"live_core_sdk_data":{"pull_data":{"options":{"default_quality":{"name":"","sdk_key":"hd","v_codec":"","resolution":"","level":0,"v_bit_rate":0,"additional_content":"","fps":0,"disable":0},"qualities":[]},"stream_data":"{\"common\":{\"session_id\":\"037-20240229074557800FDD0B23CF2EBFEA95\",\"stream\":\"114699159197975014\",\"rule_ids\":\"{\\\"ab_version_trace\\\":null,\\\"sched\\\":\\\"{\\\\\\\"result\\\\\\\":{\\\\\\\"hit\\\\\\\":\\\\\\\"default\\\\\\\",\\\\\\\"cdn\\\\\\\":547}}\\\"}\",\"app_id\":\"100102\"},\"data\":{\"ld\":{\"main\":{\"flv\":\"http://pull-flv-l26.douyincdn.com/third/stream-114699159197975014_ld.flv?expire=65e90036\u0026sign=efccd9b5b05039aa55b883f07612af8b\",\"hls\":\"http://pull-hls-l26.douyincdn.com/third/stream-114699159197975014_ld.m3u8?expire=65e90036\u0026sign=38b7513e46d7fe5a1823c133471d3c93\",\"cmaf\":\"\",\"dash\":\"\",\"lls\":\"http://pull-lls-l26.douyincdn.com/third/stream-114699159197975014_ld.sdp?expire=65e90036\u0026sign=af69fd643ed0faee45aecf205d4a3c01\",\"tsl\":\"\",\"tile\":\"\",\"sdk_params\":\"{\\\"VCodec\\\":\\\"h264\\\",\\\"vbitrate\\\":1000000,\\\"resolution\\\":\\\"960x540\\\",\\\"gop\\\":4,\\\"drType\\\":\\\"sdr\\\"}\"}},\"ao\":{\"main\":{\"flv\":\"http://pull-flv-l26.douyincdn.com/third/stream-114699159197975014.flv?expire=65e90036\u0026sign=d9c9afbd084a02f5744bb83f4f73103e\u0026only_audio=1\",\"hls\":\"\",\"cmaf\":\"\",\"dash\":\"\",\"lls\":\"\",\"tsl\":\"\",\"tile\":\"\",\"sdk_params\":\"{\\\"VCodec\\\":\\\"h264\\\",\\\"vbitrate\\\":0,\\\"resolution\\\":\\\"\\\",\\\"gop\\\":4,\\\"drType\\\":\\\"sdr\\\"}\"}},\"md\":{\"main\":{\"flv\":\"https://pull-flv-l26-admin.douyincdn.com/third/stream-114699159197975014_md.flv?expire=65e90036\u0026sign=18c7ab5b536f08b76e141bc3b4a515e2\",\"hls\":\"http://pull-hls-l26.douyincdn.com/third/stream-114699159197975014_md.m3u8?expire=65e90036\u0026sign=994bd01761f411f96e24aaacb99a000c\",\"cmaf\":\"\",\"dash\":\"\",\"lls\":\"http://pull-lls-l26.douyincdn.com/third/stream-114699159197975014_md.sdp?expire=65e90036\u0026sign=ee2dd0602401c29a1c8cfcbd9dfd3277\",\"tsl\":\"\",\"tile\":\"\",\"sdk_params\":\"{\\\"VCodec\\\":\\\"h264\\\",\\\"vbitrate\\\":800000,\\\"resolution\\\":\\\"640x360\\\",\\\"gop\\\":4,\\\"drType\\\":\\\"sdr\\\"}\"}},\"origin\":{\"main\":{\"flv\":\"http://pull-flv-l26.douyincdn.com/third/stream-114699159197975014_or4.flv?expire=65e90036\u0026sign=74cb69cf7d291dae21a0e72a852d81c2\",\"hls\":\"http://pull-hls-l26.douyincdn.com/third/stream-114699159197975014_or4.m3u8?expire=65e90036\u0026sign=fbc47970402a410858ad701375ef3a30\",\"cmaf\":\"\",\"dash\":\"\",\"lls\":\"http://pull-lls-l26.douyincdn.com/third/stream-114699159197975014_or4.sdp?expire=65e90036\u0026sign=835764f0f651c1f608962ac2e6b6e74c\",\"tsl\":\"\",\"tile\":\"\",\"sdk_params\":\"{\\\"VCodec\\\":\\\"h264\\\",\\\"vbitrate\\\":7117000,\\\"resolution\\\":\\\"1920x1080\\\",\\\"gop\\\":4,\\\"drType\\\":\\\"sdr\\\"}\"}},\"uhd\":{\"main\":{\"flv\":\"http://pull-flv-l26.douyincdn.com/third/stream-114699159197975014_uhd.flv?expire=65e90036\u0026sign=855437ae582cc8338a756542167aa26d\",\"hls\":\"http://pull-hls-l26.douyincdn.com/third/stream-114699159197975014_uhd.m3u8?expire=65e90036\u0026sign=203f18accd970d3bab50650321905d82\",\"cmaf\":\"\",\"dash\":\"\",\"lls\":\"http://pull-lls-l26.douyincdn.com/third/stream-114699159197975014_uhd.sdp?expire=65e90036\u0026sign=6d14c8aa93fa99c38bc4254bcecb69dd\",\"tsl\":\"\",\"tile\":\"\",\"sdk_params\":\"{\\\"VCodec\\\":\\\"h264\\\",\\\"vbitrate\\\":6000000,\\\"resolution\\\":\\\"1920x1080\\\",\\\"gop\\\":4,\\\"drType\\\":\\\"sdr\\\"}\"}},\"hd\":{\"main\":{\"flv\":\"http://pull-flv-l26.douyincdn.com/third/stream-114699159197975014_hd.flv?expire=65e90036\u0026sign=a37489d89fd67adee8b45f725d303495\",\"hls\":\"http://pull-hls-l26.douyincdn.com/third/stream-114699159197975014_hd.m3u8?expire=65e90036\u0026sign=cb2615212306aaf7e1adf03d7ed3bcd2\",\"cmaf\":\"\",\"dash\":\"\",\"lls\":\"http://pull-lls-l26.douyincdn.com/third/stream-114699159197975014_hd.sdp?expire=65e90036\u0026sign=2d7e299ab7f422c0df325651957825ee\",\"tsl\":\"\",\"tile\":\"\",\"sdk_params\":\"{\\\"VCodec\\\":\\\"h264\\\",\\\"vbitrate\\\":4000000,\\\"resolution\\\":\\\"1280x720\\\",\\\"gop\\\":4,\\\"drType\\\":\\\"sdr\\\"}\"}},\"sd\":{\"main\":{\"flv\":\"http://pull-flv-l26.douyincdn.com/third/stream-114699159197975014_sd.flv?expire=65e90036\u0026sign=5da4bbcde0edba5dd27b11951c0a72cb\",\"hls\":\"http://pull-hls-l26.douyincdn.com/third/stream-114699159197975014_sd.m3u8?expire=65e90036\u0026sign=74dc48d61f34c411c9be2016565f67bb\",\"cmaf\":\"\",\"dash\":\"\",\"lls\":\"http://pull-lls-l26.douyincdn.com/third/stream-114699159197975014_sd.sdp?expire=65e90036\u0026sign=f344c957571ecf63a2cc9bfdaad24d63\",\"tsl\":\"\",\"tile\":\"\",\"sdk_params\":\"{\\\"VCodec\\\":\\\"h264\\\",\\\"vbitrate\\\":2000000,\\\"resolution\\\":\\\"1280x720\\\",\\\"gop\\\":4,\\\"drType\\\":\\\"sdr\\\"}\"}}}}"}},"extra":{"height":1080,"width":1920,"fps":0,"max_bitrate":0,"min_bitrate":0,"default_bitrate":0,"bitrate_adapt_strategy":0,"anchor_interact_profile":0,"audience_interact_profile":0,"hardware_encode":false,"video_profile":0,"h265_enable":false,"gop_sec":0,"bframe_enable":false,"roi":false,"sw_roi":false,"bytevc1_enable":false},"pull_datas":{}},"mosaic_status":0,"mosaic_status_str":"0","admin_user_ids":[],"admin_user_ids_str":[],"owner":{"id_str":"3802583057567792","sec_uid":"MS4wLjABAAAA5Eo5q2JwRwQrndv9CqO4QKu7p376M5862SWtLhQBp4PXGROhGGPbd9wVVQ4WzKgR","nickname":"杜洛华(无畏契约）","avatar_thumb":{"url_list":["https://p3.douyinpic.com/aweme/100x100/aweme-avatar/tos-cn-avt-0015_af340f28a756d28d98a3d721dea1ac74.jpeg?from=3067671334","https://p6.douyinpic.com/aweme/100x100/aweme-avatar/tos-cn-avt-0015_af340f28a756d28d98a3d721dea1ac74.jpeg?from=3067671334","https://p11.douyinpic.com/aweme/100x100/aweme-avatar/tos-cn-avt-0015_af340f28a756d28d98a3d721dea1ac74.jpeg?from=3067671334"]},"follow_info":{"follow_status":0,"follow_status_str":"0"}},"live_room_mode":1,"stats":{"total_user_desp":"","like_count":0,"total_user_str":"3万+","user_count_str":"1608"},"has_commerce_goods":false,"linker_map":{},"room_view_stats":{"is_hidden":false,"display_short":"1608","display_middle":"1608","display_long":"1608在线观众","display_value":1608,"display_version":1663849727,"incremental":false,"display_type":1,"display_short_anchor":"1608","display_middle_anchor":"1608","display_long_anchor":"1608在线观众"},"ecom_data":{"reds_show_infos":[],"room_cart_v2":{"show_cart":2}},"AnchorABMap":{},"like_count":0,"owner_user_id_str":"","paid_live_data":{"paid_type":0,"view_right":0,"duration":0,"delivery":0,"need_delivery_notice":false,"anchor_right":0,"pay_ab_type":1,"privilege_info":{},"privilege_info_map":{}},"others":{"web_live_port_optimization":{"strategy_config":{"background":{"strategy_type":1,"use_config_duration":false,"pause_monitor_duration":"1800"},"detail":{"strategy_type":1,"use_config_duration":false,"pause_monitor_duration":"1800"},"tab":{"strategy_type":1,"use_config_duration":false,"pause_monitor_duration":"1800"}},"strategy_extra":""},"lvideo_item_id":0}},"tag_name":"","uniq_id":"92235129220","web_rid":"16049748331","is_recommend":1,"title_type":2,"cover_type":1},
    """.trimIndent()

    val matchResult = Douyin.AVATAR_REGEX.toRegex().find(content) ?: throw IllegalArgumentException("Invalid content")
    assertEquals(
      "failed to match avatar url",
      matchResult.groupValues.last(),
      "https://p26.douyinpic.com/aweme/100x100/aweme-avatar/tos-cn-avt-0015_91c6d02d076a3943c80db5664c9541bb.jpeg?from=3067671334"
    )
  }

  @Test
  fun testLive() = runTest {
    val app = App(Json).apply {
      updateConfig(AppConfig())
    }
    val extractor = DouyinExtractor(app.client, app.json, "https://live.douyin.com/557481980778")
    val info = extractor.extract()
    println(info)
    assertNotNull("failed to extract", info)
  }
}