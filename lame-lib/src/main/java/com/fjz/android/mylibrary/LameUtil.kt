package com.fjz.android.mylibrary

/*
* Created by Jinzhen Feng on 2026/2/28.
* Copyright (c) 2026 . All rights reserved.
*/
class LameUtil {
    companion object {
        // 这里的 "mylibrary" 必须与你 CMakeLists.txt 中 project()
        // 或 add_library() 定义的名字一致
        init {
            System.loadLibrary("mylibrary")
        }
    }

    /**
     * 初始化 LAME 编码器
     * @param inSampleRate  输入采样率 (Hz)
     * @param outChannel    输出声道数
     * @param outSampleRate 输出采样率 (Hz)
     * @param outBitrate    输出比特率 (kbps)
     * @param quality       质量 (0-9, 0最好, 2推荐, 7及以上较差)
     */
    external fun init(
        inSampleRate: Int,
        outChannel: Int,
        outSampleRate: Int,
        outBitrate: Int,
        quality: Int
    )

    /**
     * 编码 PCM 数据为 MP3 数据
     * @param bufferLeft  左声道数据 (ShortArray)
     * @param bufferRight 右声道数据 (如果是单声道，可传入相同的 buffer 或 null)
     * @param samples     采样点数量
     * @param mp3buf      存储编码后的 MP3 数据的字节数组
     * @return 实际写入 mp3buf 的字节数
     */
    external fun encode(
        bufferLeft: ShortArray,
        bufferRight: ShortArray?,
        samples: Int,
        mp3buf: ByteArray
    ): Int

    /**
     * 刷新 LAME 缓冲区（在编码结束前调用，获取最后一部分 MP3 数据）
     * @param mp3buf 存储最后 MP3 数据的字节数组
     * @return 实际写入的字节数
     */
    external fun flush(mp3buf: ByteArray): Int

    /**
     * 释放 LAME 资源
     */
    external fun close()

}