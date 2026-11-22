import { AnniversaryDto } from '../../model/Anniversary';

/** 服务端统一信封。失败响应的 data 结构可能与成功结果不同，必须先判断 code。 */
export interface StatusDto {
  code: number;
  msg?: string;
}

export interface TokenDto {
  token: string;
  token_expire_time: number;
  refresh_token: string;
  refresh_token_expire_time: number;
}

export interface ServerUserDto {
  id: number;
  nickname?: string;
  account?: string;
  email?: string;
}

export interface LoginDataDto {
  user: ServerUserDto;
  token: TokenDto;
}

export interface RefreshDataDto {
  token: TokenDto;
}

export interface UserUpdateDataDto {
  user: ServerUserDto;
}

export interface ListDataDto {
  list: AnniversaryDto[];
}

export interface SaveDataDto {
  date: AnniversaryDto;
}

export interface LoginEnvelope {
  code: number;
  msg?: string;
  data?: LoginDataDto;
}

export interface RefreshEnvelope {
  code: number;
  msg?: string;
  data?: RefreshDataDto;
}

export interface UserEnvelope {
  code: number;
  msg?: string;
  data?: UserUpdateDataDto;
}

export interface ListEnvelope {
  code: number;
  msg?: string;
  data?: ListDataDto;
}

export interface SaveEnvelope {
  code: number;
  msg?: string;
  data?: SaveDataDto;
}

/** 错误类型只用于分流提示与断网判断，具体文案交给界面层。 */
export class ApiError extends Error {
  kind: string;
  code: number;

  constructor(kind: string, message: string, code: number = 0) {
    super(message);
    this.kind = kind;
    this.code = code;
  }

  static network(): ApiError {
    return new ApiError('network', 'network');
  }

  static authentication(): ApiError {
    return new ApiError('auth', 'auth');
  }

  static invalidData(): ApiError {
    return new ApiError('invalid', 'invalid');
  }

  static server(code: number, message: string): ApiError {
    return new ApiError('server', message, code);
  }
}
