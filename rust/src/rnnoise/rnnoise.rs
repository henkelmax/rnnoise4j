use jni::{JNIEnv};
use jni::objects::{JClass, JObject, JShortArray, JValue};
use jni::sys::{jlong, jshort};
use nnnoiseless::DenoiseState;

const FLOAT_SHORT_SCALE: f32 = (i16::MAX - 1i16) as f32;

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_rnnoise4j_Denoiser_createDenoiser(_env: JNIEnv, _class: JClass) -> jlong {
    let denoiser = DenoiseState::new();
    return create_pointer(denoiser);
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_rnnoise4j_Denoiser_denoise<'a>(mut env: JNIEnv<'a>, obj: JObject<'a>, input: JShortArray<'a>) -> JShortArray<'a> {
    let denoiser = match get_denoiser(&mut env, &obj) {
        Some(denoiser) => denoiser,
        None => {
            return JShortArray::from(JObject::null());
        }
    };

    let input_length = match env.get_array_length(&input) {
        Ok(input_length) => input_length as usize,
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed to get input length: {}", e));
            return JShortArray::from(JObject::null());
        }
    };

    if input_length % DenoiseState::FRAME_SIZE != 0 {
        let _ = env.throw(("java/lang/IllegalArgumentException", format!("Input length must be a multiple of {}", DenoiseState::FRAME_SIZE)));
        return JShortArray::from(JObject::null());
    }

    let mut short_array = vec![0i16 as jshort; input_length];

    match env.get_short_array_region(input, 0, &mut short_array) {
        Ok(_) => {}
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed to convert short array: {}", e));
            return JShortArray::from(JObject::null());
        }
    };

    let input_float_array: Vec<f32> = short_array.iter().map(|sample| *sample as f32).collect();

    let mut output_float_array = vec![0f32; input_length];
    let mut out_buf = [0f32; DenoiseState::FRAME_SIZE];
    let mut offset = 0;
    for chunk in input_float_array.chunks(DenoiseState::FRAME_SIZE) {
        denoiser.process_frame(&mut out_buf, chunk);

        output_float_array[offset..offset + chunk.len()].copy_from_slice(&out_buf[..chunk.len()]);

        offset += chunk.len();
    }

    let mut max = f32::MIN;
    let mut min = f32::MAX;

    for i in 0..input_length {
        if output_float_array[i] > max {
            max = output_float_array[i];
        }
        if output_float_array[i] < min {
            min = output_float_array[i];
        }
    }

    let scale = f32::min(1f32, FLOAT_SHORT_SCALE / f32::max(f32::abs(max), f32::abs(min)));

    for i in 0..input_length {
        short_array[i] = (output_float_array[i] * scale) as jshort;
    }

    let output_short_array = match env.new_short_array(input_length as i32) {
        Ok(array) => array,
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed to create short array: {}", e));
            return JShortArray::from(JObject::null());
        }
    };
    match env.set_short_array_region(&output_short_array, 0, short_array.as_slice()) {
        Ok(_) => {}
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed populate short array: {}", e));
            return JShortArray::from(JObject::null());
        }
    }
    return output_short_array;
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_rnnoise4j_Denoiser_destroyDenoiser(mut env: JNIEnv, obj: JObject) {
    let pointer = get_pointer(&mut env, &obj);

    if pointer == 0 {
        return;
    }

    let _ = unsafe { Box::from_raw(pointer as *mut DenoiseState) };
    let _ = env.set_field(obj, "denoiser", "J", JValue::from(jlong::from(0)));
}

fn get_pointer(env: &mut JNIEnv, obj: &JObject) -> jlong {
    let pointer = match env.get_field(obj, "denoiser", "J") {
        Ok(pointer) => pointer,
        Err(e) => {
            throw_runtime_exception(env, format!("Failed to get denoiser pointer: {}", e));
            return 0;
        }
    };
    let long = match pointer.j() {
        Ok(long) => long,
        Err(e) => {
            throw_runtime_exception(env, format!("Failed to convert denoiser pointer to long: {}", e));
            return 0;
        }
    };
    return long;
}

fn get_denoiser_from_pointer(pointer: jlong) -> &'static mut DenoiseState<'static> {
    let denoiser = unsafe { &mut *(pointer as *mut DenoiseState) };
    return denoiser;
}

fn get_denoiser(env: &mut JNIEnv, obj: &JObject) -> Option<&'static mut DenoiseState<'static>> {
    let pointer = get_pointer(env, obj);
    if pointer == 0 {
        let _ = env.throw(("java/lang/IllegalStateException", "Denoiser is closed"));
        return None;
    }
    return Some(get_denoiser_from_pointer(pointer));
}

fn create_pointer(denoiser: Box<DenoiseState>) -> jlong {
    let raw = Box::into_raw(denoiser);
    return raw as jlong;
}

fn throw_runtime_exception(env: &mut JNIEnv, message: String) {
    let _ = env.throw(("java/lang/RuntimeException", message));
}