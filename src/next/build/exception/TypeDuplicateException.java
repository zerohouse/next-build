package next.build.exception;

public class TypeDuplicateException extends Exception {

	public TypeDuplicateException(String id, Class<?> type, Class<?> type2) {
		super(String.format("id:%s을(를) %s, %s 타입이 중복 사용합니다.", id, type, type2));
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
