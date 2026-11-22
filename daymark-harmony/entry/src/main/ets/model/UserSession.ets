/** 本机会话；时间均为毫秒时间戳，与两端服务端字段保持一致。 */
export interface SessionDto {
  id?: number;
  name?: string;
  token?: string;
  tokenExpire?: number;
  refreshToken?: string;
  refreshExpire?: number;
  account?: string;
  email?: string;
}

export class UserSession {
  id: number = 0;
  name: string = '';
  token: string = '';
  tokenExpire: number = 0;
  refreshToken: string = '';
  refreshExpire: number = 0;
  account: string = '';
  email: string = '';

  copy(): UserSession {
    const result: UserSession = new UserSession();
    result.id = this.id;
    result.name = this.name;
    result.token = this.token;
    result.tokenExpire = this.tokenExpire;
    result.refreshToken = this.refreshToken;
    result.refreshExpire = this.refreshExpire;
    result.account = this.account;
    result.email = this.email;
    return result;
  }

  static fromJson(json: string): UserSession | null {
    if (json.length === 0) {
      return null;
    }
    try {
      const dto: SessionDto = JSON.parse(json) as SessionDto;
      const result: UserSession = new UserSession();
      result.id = dto.id === undefined ? 0 : dto.id;
      result.name = dto.name === undefined ? '' : dto.name;
      result.token = dto.token === undefined ? '' : dto.token;
      result.tokenExpire = dto.tokenExpire === undefined ? 0 : dto.tokenExpire;
      result.refreshToken = dto.refreshToken === undefined ? '' : dto.refreshToken;
      result.refreshExpire = dto.refreshExpire === undefined ? 0 : dto.refreshExpire;
      result.account = dto.account === undefined ? '' : dto.account;
      result.email = dto.email === undefined ? '' : dto.email;
      return result.id > 0 ? result : null;
    } catch (err) {
      return null;
    }
  }

  toJson(): string {
    const dto: SessionDto = {
      id: this.id,
      name: this.name,
      token: this.token,
      tokenExpire: this.tokenExpire,
      refreshToken: this.refreshToken,
      refreshExpire: this.refreshExpire,
      account: this.account,
      email: this.email
    };
    return JSON.stringify(dto);
  }

  /** 令牌是否仍然可用；与 iOS 一样提前 60 秒视为过期。 */
  tokenValid(now: number): boolean {
    return this.token.length > 0 && this.tokenExpire > now + 60000;
  }
}
