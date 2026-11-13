package com.yuzhijie.career.trace.module.job.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuzhijie.career.trace.common.api.PageResult;
import com.yuzhijie.career.trace.common.exception.BizException;
import com.yuzhijie.career.trace.common.exception.ErrorCodes;
import com.yuzhijie.career.trace.common.util.Pages;
import com.yuzhijie.career.trace.module.job.mapper.JobMapper;
import com.yuzhijie.career.trace.module.job.mapper.JobTagMapper;
import com.yuzhijie.career.trace.module.job.model.JobRow;
import com.yuzhijie.career.trace.module.job.model.JobTagRow;
import com.yuzhijie.career.trace.module.job.vo.JobCompanyVO;
import com.yuzhijie.career.trace.module.job.vo.JobDetailVO;
import com.yuzhijie.career.trace.module.job.vo.JobListItemVO;
import com.yuzhijie.career.trace.module.job.vo.JobTagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobMapper jobMapper;
    private final JobTagMapper jobTagMapper;

    public PageResult<JobListItemVO> page(Integer page, Integer size, String city, String keyword, Long companyId) {
        int pageNo = Pages.page(page);
        int pageSize = Pages.size(size);
        Page<JobRow> result = jobMapper.selectJobPage(new Page<>(pageNo, pageSize),
                trimToNull(city), trimToNull(keyword), companyId);
        List<JobListItemVO> records = result.getRecords().stream()
                .map(row -> new JobListItemVO(
                        row.getJobId(),
                        row.getTitle(),
                        row.getCompanyId(),
                        row.getCompanyName(),
                        row.getCity(),
                        row.getSalaryMin(),
                        row.getSalaryMax(),
                        row.getStatus()))
                .toList();
        return new PageResult<>(result.getTotal(), pageNo, pageSize, records);
    }

    public JobDetailVO detail(Long jobId) {
        JobRow row = jobMapper.selectJobRowById(jobId);
        if (row == null) {
            throw BizException.notFound(ErrorCodes.JOB_NOT_FOUND, "该岗位已下线");
        }
        List<JobTagVO> tags = jobTagMapper.selectByJobId(jobId).stream()
                .map(this::toJobTagVO)
                .toList();

        JobDetailVO detail = new JobDetailVO();
        detail.setJobId(row.getJobId());
        detail.setExternalId(row.getExternalId());
        detail.setSource(row.getSource());
        detail.setTitle(row.getTitle());
        if (row.getCompanyId() != null) {
            detail.setCompany(new JobCompanyVO(row.getCompanyId(), row.getCompanyName(),
                    row.getCompanyIndustry(), row.getCompanySize(), row.getCompanyStage()));
        }
        detail.setCity(row.getCity());
        detail.setSalaryMin(row.getSalaryMin());
        detail.setSalaryMax(row.getSalaryMax());
        detail.setJdText(row.getJdText());
        detail.setStatus(row.getStatus());
        detail.setTags(tags);
        return detail;
    }

    private JobTagVO toJobTagVO(JobTagRow row) {
        return new JobTagVO(row.getTagId(), row.getTagName(), row.getTagCategory(),
                row.getWeight(), row.getRequiredLevel());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
