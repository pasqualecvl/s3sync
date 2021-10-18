package it.pasqualecavallo.s3sync.web.dto.response;

public class RestBaseResponse {

	private ResponseStatus status = ResponseStatus.OK;
	
	private String errorCode;
	private String errorMessage;
	
	public enum ResponseStatus {
		OK,KO;
	}

	public void setStatus(ResponseStatus status) {
		this.status = status;
	}
	
	public ResponseStatus getStatus() {
		return status;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	public String getErrorCode() {
		return errorCode;
	}
	
	public void setError(ErrorMessage errorMessage, Iterable<String> placeholders) {
		this.errorCode = errorMessage.name();
		this.status = ResponseStatus.KO;
		if(placeholders == null) {
			this.errorMessage = errorMessage.value();
		} else {
			String errorMessageString = errorMessage.value();
			for(String placeholder : placeholders) {
				errorMessageString = errorMessageString.replaceFirst("\\{\\}", placeholder);
			}
			this.errorMessage = errorMessageString;
		}
	}
	
	public enum ErrorMessage {
		E400_BAD_REQUEST("Bad request: {}"),
		E400_RESERVED_KEYWORK("Reserved keyword: {}"),
		E500_SYNC_ERROR("Synchronization error: {}"),
		E500_GENERIC_ERROR("Generic technical error");
	
		private String value;
		
		private ErrorMessage(String value) {
			this.value = value;
		}
		
		public String value() {
			return value;
		}
	}
}
