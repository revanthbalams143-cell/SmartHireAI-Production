package com.smarthire.backend.platform.analytics.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RecruiterAnalyticsResponse {
    private Summary summary = new Summary();
    private List<CandidateItem> ranking = new ArrayList<>();
    private List<MetricItem> dimensionAverages = new ArrayList<>();
    private List<TrendPoint> trends = new ArrayList<>();
    private List<SkillCount> skills = new ArrayList<>();
    private List<SkillCount> weakSkills = new ArrayList<>();
    public Summary getSummary(){return summary;} public void setSummary(Summary v){summary=v;}
    public List<CandidateItem> getRanking(){return ranking;} public void setRanking(List<CandidateItem> v){ranking=v;}
    public List<MetricItem> getDimensionAverages(){return dimensionAverages;} public void setDimensionAverages(List<MetricItem> v){dimensionAverages=v;}
    public List<TrendPoint> getTrends(){return trends;} public void setTrends(List<TrendPoint> v){trends=v;}
    public List<SkillCount> getSkills(){return skills;} public void setSkills(List<SkillCount> v){skills=v;}
    public List<SkillCount> getWeakSkills(){return weakSkills;} public void setWeakSkills(List<SkillCount> v){weakSkills=v;}
    public static class Summary { private int totalCandidates,scoredCandidates,averageInterviewScore,averageAtsScore,shortlistedCandidates,shortlistRate; public int getTotalCandidates(){return totalCandidates;} public void setTotalCandidates(int v){totalCandidates=v;} public int getScoredCandidates(){return scoredCandidates;} public void setScoredCandidates(int v){scoredCandidates=v;} public int getAverageInterviewScore(){return averageInterviewScore;} public void setAverageInterviewScore(int v){averageInterviewScore=v;} public int getAverageAtsScore(){return averageAtsScore;} public void setAverageAtsScore(int v){averageAtsScore=v;} public int getShortlistedCandidates(){return shortlistedCandidates;} public void setShortlistedCandidates(int v){shortlistedCandidates=v;} public int getShortlistRate(){return shortlistRate;} public void setShortlistRate(int v){shortlistRate=v;} }
    public static class CandidateItem { private Long candidateId; private String name,status; private int interviewScore,atsScore,readiness; private LocalDateTime lastInterview; public Long getCandidateId(){return candidateId;} public void setCandidateId(Long v){candidateId=v;} public String getName(){return name;} public void setName(String v){name=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public int getInterviewScore(){return interviewScore;} public void setInterviewScore(int v){interviewScore=v;} public int getAtsScore(){return atsScore;} public void setAtsScore(int v){atsScore=v;} public int getReadiness(){return readiness;} public void setReadiness(int v){readiness=v;} public LocalDateTime getLastInterview(){return lastInterview;} public void setLastInterview(LocalDateTime v){lastInterview=v;} }
    public static class MetricItem { private String label; private int value; public MetricItem(){} public MetricItem(String l,int v){label=l;value=v;} public String getLabel(){return label;} public void setLabel(String v){label=v;} public int getValue(){return value;} public void setValue(int v){value=v;} }
    public static class TrendPoint { private LocalDateTime date; private int score; public LocalDateTime getDate(){return date;} public void setDate(LocalDateTime v){date=v;} public int getScore(){return score;} public void setScore(int v){score=v;} }
    public static class SkillCount { private String skill; private int count; public SkillCount(){} public SkillCount(String s,int c){skill=s;count=c;} public String getSkill(){return skill;} public void setSkill(String v){skill=v;} public int getCount(){return count;} public void setCount(int v){count=v;} }
}
