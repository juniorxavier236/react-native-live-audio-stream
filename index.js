import { NativeModules, NativeEventEmitter } from 'react-native';
const { RNLiveAudioStream } = NativeModules;
const EventEmitter = new NativeEventEmitter(RNLiveAudioStream);

const Audio = {};

Audio.initRecord = options => RNLiveAudioStream.initRecord(options);
Audio.startRecord = () => RNLiveAudioStream.startRecord();
Audio.stopRecord = () => RNLiveAudioStream.stopRecord();

Audio.initPlay = options => RNLiveAudioStream.initPlay(options);
Audio.startPlay = () => RNLiveAudioStream.startPlay();
Audio.stopPlay = () => RNLiveAudioStream.stopPlay();
Audio.writePlay = base64 => RNLiveAudioStream.writePlay(base64);
Audio.setVolumePlay = options => RNLiveAudioStream.setVolumePlay(options);

Audio.setSpeakerphoneOn = options => RNLiveAudioStream.setSpeakerphoneOn(options);
Audio.setMicrophoneMute = options => RNLiveAudioStream.setMicrophoneMute(options);

const eventsMap = {
  data: 'data'
};

Audio.on = (event, callback) => {
  const nativeEvent = eventsMap[event];
  if (!nativeEvent) {
    throw new Error('Invalid event');
  }
  EventEmitter.removeAllListeners(nativeEvent);
  return EventEmitter.addListener(nativeEvent, callback);
};

export default Audio;
