package com.xteam.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xteam.video.entity.VideoTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VideoTaskMapper extends BaseMapper<VideoTask> {

    List<VideoTask> selectPendingTasks();

    List<VideoTask> selectByUserId(@Param("userId") String userId);

    VideoTask selectByIdWithLock(@Param("id") Long id);
}