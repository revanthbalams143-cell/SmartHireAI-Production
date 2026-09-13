package com.smarthire.backend.platform.analytics.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CandidateAnalyticsResponse {
    private Summary summary = new Summary();
    private List<HistoryItem> history = new ArrayList<>();
    private List<TrendPoint> trends = new ArrayList<>();
    private List<MetricItem> skillAnalytics = new ArrayList<>();
    private List<WeakArea> weakAreas = new ArrayList<>();
    private List<ImprovementItem> improvement = new ArrayList<>();
    private Feedback feedback = new Feedback();
    private List<ResumeSkill> resumeSkills = new ArrayList<>();

    public Summary getSummary(){return summary;} public void setSummary(Summary v){summary=v;}
    public List<HistoryItem> getHistory(){return history;} public void setHistory(List<HistoryItem> v){history=v;}
    public List<TrendPoint> getTrends(){return trends;} public void setTrends(List<TrendPoint> v){trends=v;}
    public List<MetricItem> getSkillAnalytics(){return skillAnalytics;} public void setSkillAnalytics(List<MetricItem> v){skillAnalytics=v;}
    public List<WeakArea> getWeakAreas(){return weakAreas;} public void setWeakAreas(List<WeakArea> v){weakAreas=v;}
    public List<ImprovementItem> getImprovement(){return improvement;} public void setImprovement(List<ImprovementItem> v){improvement=v;}
    public Feedback getFeedback(){return feedback;} public void setFeedback(Feedback v){feedback=v;}
    public List<ResumeSkill> getResumeSkills(){return resumeSkills;} public void setResumeSkills(List<ResumeSkill> v){resumeSkills=v;}

    public static class Summary {
        private int completedInterviews, evaluatedInterviews, latestOverallScore, averageOverallScore, improvementDelta;
        private double averageCommunication, averageConfidence, averageTechnical, averageProfessionalism, averageProblemSolving;
        private String rating = "Not enough data";
        private String latestJobRole = "";
        private LocalDateTime latestDate;
        private boolean reportAvailable;
        public int getCompletedInterviews(){return completedInterviews;} public void setCompletedInterviews(int v){completedInterviews=v;}
        public int getEvaluatedInterviews(){return evaluatedInterviews;} public void setEvaluatedInterviews(int v){evaluatedInterviews=v;}
        public int getLatestOverallScore(){return latestOverallScore;} public void setLatestOverallScore(int v){latestOverallScore=v;}
        public int getAverageOverallScore(){return averageOverallScore;} public void setAverageOverallScore(int v){averageOverallScore=v;}
        public int getImprovementDelta(){return improvementDelta;} public void setImprovementDelta(int v){improvementDelta=v;}
        public double getAverageCommunication(){return averageCommunication;} public void setAverageCommunication(double v){averageCommunication=v;}
        public double getAverageConfidence(){return averageConfidence;} public void setAverageConfidence(double v){averageConfidence=v;}
        public double getAverageTechnical(){return averageTechnical;} public void setAverageTechnical(double v){averageTechnical=v;}
        public double getAverageProfessionalism(){return averageProfessionalism;} public void setAverageProfessionalism(double v){averageProfessionalism=v;}
        public double getAverageProblemSolving(){return averageProblemSolving;} public void setAverageProblemSolving(double v){averageProblemSolving=v;}
        public String getRating(){return rating;} public void setRating(String v){rating=v;}
        public String getLatestJobRole(){return latestJobRole;} public void setLatestJobRole(String v){latestJobRole=v;}
        public LocalDateTime getLatestDate(){return latestDate;} public void setLatestDate(LocalDateTime v){latestDate=v;}
        public boolean isReportAvailable(){return reportAvailable;} public void setReportAvailable(boolean v){reportAvailable=v;}
    }
    public static class HistoryItem {
        private Long interviewId; private String jobRole, interviewType, status, rating; private LocalDateTime date; private Integer overallScore, communicationScore, confidenceScore, technicalScore, professionalismScore, problemSolvingScore;
        public Long getInterviewId(){return interviewId;} public void setInterviewId(Long v){interviewId=v;}
        public String getJobRole(){return jobRole;} public void setJobRole(String v){jobRole=v;}
        public String getInterviewType(){return interviewType;} public void setInterviewType(String v){interviewType=v;}
        public String getStatus(){return status;} public void setStatus(String v){status=v;}
        public String getRating(){return rating;} public void setRating(String v){rating=v;}
        public LocalDateTime getDate(){return date;} public void setDate(LocalDateTime v){date=v;}
        public Integer getOverallScore(){return overallScore;} public void setOverallScore(Integer v){overallScore=v;}
        public Integer getCommunicationScore(){return communicationScore;} public void setCommunicationScore(Integer v){communicationScore=v;}
        public Integer getConfidenceScore(){return confidenceScore;} public void setConfidenceScore(Integer v){confidenceScore=v;}
        public Integer getTechnicalScore(){return technicalScore;} public void setTechnicalScore(Integer v){technicalScore=v;}
        public Integer getProfessionalismScore(){return professionalismScore;} public void setProfessionalismScore(Integer v){professionalismScore=v;}
        public Integer getProblemSolvingScore(){return problemSolvingScore;} public void setProblemSolvingScore(Integer v){problemSolvingScore=v;}
    }
    public static class TrendPoint {
        private LocalDateTime date; private int overall, communication, confidence, technical, professionalism, problemSolving;
        public LocalDateTime getDate(){return date;} public void setDate(LocalDateTime v){date=v;}
        public int getOverall(){return overall;} public void setOverall(int v){overall=v;}
        public int getCommunication(){return communication;} public void setCommunication(int v){communication=v;}
        public int getConfidence(){return confidence;} public void setConfidence(int v){confidence=v;}
        public int getTechnical(){return technical;} public void setTechnical(int v){technical=v;}
        public int getProfessionalism(){return professionalism;} public void setProfessionalism(int v){professionalism=v;}
        public int getProblemSolving(){return problemSolving;} public void setProblemSolving(int v){problemSolving=v;}
    }
    public static class MetricItem { private String label; private int value; private String source; public MetricItem(){} public MetricItem(String l,int v,String s){label=l;value=v;source=s;} public String getLabel(){return label;} public void setLabel(String v){label=v;} public int getValue(){return value;} public void setValue(int v){value=v;} public String getSource(){return source;} public void setSource(String v){source=v;} }
    public static class WeakArea { private String label, reason, source; private int score; public WeakArea(){} public WeakArea(String l,int s,String r,String src){label=l;score=s;reason=r;source=src;} public String getLabel(){return label;} public void setLabel(String v){label=v;} public int getScore(){return score;} public void setScore(int v){score=v;} public String getReason(){return reason;} public void setReason(String v){reason=v;} public String getSource(){return source;} public void setSource(String v){source=v;} }
    public static class ImprovementItem { private String label; private int baseline, latest, delta; public ImprovementItem(){} public ImprovementItem(String l,int b,int n){label=l;baseline=b;latest=n;delta=n-b;} public String getLabel(){return label;} public void setLabel(String v){label=v;} public int getBaseline(){return baseline;} public void setBaseline(int v){baseline=v;} public int getLatest(){return latest;} public void setLatest(int v){latest=v;} public int getDelta(){return delta;} public void setDelta(int v){delta=v;} }
    public static class Feedback { private List<String> strengths=new ArrayList<>(), weaknesses=new ArrayList<>(), improvementSuggestions=new ArrayList<>(), practiceRecommendations=new ArrayList<>(), learningResources=new ArrayList<>(); private String recommendation=""; public List<String> getStrengths(){return strengths;} public void setStrengths(List<String> v){strengths=v;} public List<String> getWeaknesses(){return weaknesses;} public void setWeaknesses(List<String> v){weaknesses=v;} public List<String> getImprovementSuggestions(){return improvementSuggestions;} public void setImprovementSuggestions(List<String> v){improvementSuggestions=v;} public List<String> getPracticeRecommendations(){return practiceRecommendations;} public void setPracticeRecommendations(List<String> v){practiceRecommendations=v;} public List<String> getLearningResources(){return learningResources;} public void setLearningResources(List<String> v){learningResources=v;} public String getRecommendation(){return recommendation;} public void setRecommendation(String v){recommendation=v;} }
    public static class ResumeSkill { private String skill; private int score; private String category; public ResumeSkill(){} public ResumeSkill(String s,int v,String c){skill=s;score=v;category=c;} public String getSkill(){return skill;} public void setSkill(String v){skill=v;} public int getScore(){return score;} public void setScore(int v){score=v;} public String getCategory(){return category;} public void setCategory(String v){category=v;} }
}
