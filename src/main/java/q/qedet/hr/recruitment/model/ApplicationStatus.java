package q.qedet.hr.recruitment.model;

public enum ApplicationStatus {
    APPLIED,
    INTERVIEW,
    SELECTED,
    OFFER_SENT,
    OFFER_ACCEPTED,
    HIRED,
    REJECTED,
    WITHDRAWN,
    /** @deprecated Use {@link #APPLIED}. */
    @Deprecated
    SUBMITTED,
    /** @deprecated Use {@link #SELECTED}. */
    @Deprecated
    REVIEWING,
    /** @deprecated Use {@link #SELECTED}. */
    @Deprecated
    SHORTLISTED,
    /** @deprecated Use {@link #INTERVIEW}. */
    @Deprecated
    INTERVIEWING,
    /** @deprecated Use {@link #OFFER_SENT}. */
    @Deprecated
    OFFERED,
}
