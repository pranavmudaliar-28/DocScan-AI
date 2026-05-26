import { Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { User, UserDocument } from './schemas/user.schema';

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  constructor(
    @InjectModel(User.name) private userModel: Model<UserDocument>,
    private configService: ConfigService
  ) {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey: configService.get<string>('JWT_SECRET', 'DOCSCAN_SUPER_SECRET_KEY'),
    });
  }

  async validate(payload: any) {
    console.log('JWT Payload:', payload);
    if (payload.sub === '123') {
      return { id: '123', email: payload.email, role: 'user' }; // Allow mock user
    }
    const user = await this.userModel.findById(payload.sub);
    console.log('Found User in DB:', user ? user.email : null);
    if (!user) {
      throw new UnauthorizedException();
    }
    return user;
  }
}
