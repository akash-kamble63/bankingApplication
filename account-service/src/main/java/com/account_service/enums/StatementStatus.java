package com.account_service.enums;

public enum StatementStatus {
	REQUESTED,       // Statement generation requested
    GENERATING,      // Statement is being generated
    READY,           // Statement ready for download
    DOWNLOADED,      // Statement has been downloaded
    FAILED,          // Statement generation failed
    EXPIRED          // Statement download link expired
}
