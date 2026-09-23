import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate, Link } from 'react-router-dom';

export default function Register() {
  const [step, setStep] = useState(1);
  const [formData, setFormData] = useState({
    fullName: '', email: '', phone: '', password: '', confirmPassword: '',
    dob: '', gender: '', address: '',
    emergencyContactName: '', emergencyContactNumber: '', termsAccepted: false,
    painType: '', painRating: ''
  });
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({ ...formData, [name]: type === 'checkbox' ? checked : value });
  };

  const handleNext = () => {
    if (step === 1 && formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }
    setError('');
    setStep(step + 1);
  };

  const handleBack = () => setStep(step - 1);

  const handleRegister = async (e) => {
    e.preventDefault();
    if (!formData.termsAccepted) {
      setError('You must accept the Terms & Conditions');
      return;
    }
    setError('');
    try {
      await axios.post('http://localhost:8080/api/auth/register', formData);
      alert('Registration successful! Please log in.');
      navigate('/login');
    } catch (err) {
      setError(err.response?.data || 'Registration failed');
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col items-center pt-10 pb-20">
      <div className="w-full max-w-4xl px-4">
        {/* Header */}
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Patient Registration</h1>
            <p className="text-gray-500 mt-1">Please complete all steps to onboard a new patient to the clinic system.</p>
          </div>
          <span className="bg-blue-100 text-blue-700 px-4 py-2 rounded-full text-sm font-medium">Step {step} of 3</span>
        </div>

        {/* Progress Tracker */}
        <div className="flex justify-between items-center mb-10 relative">
          <div className="absolute left-0 top-1/2 w-full h-0.5 bg-gray-200 -z-10"></div>
          
          <div className="flex flex-col items-center">
            <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold ${step >= 1 ? 'bg-blue-600 text-white shadow-md' : 'bg-white border-2 border-gray-300 text-gray-400'}`}>👤</div>
            <span className={`text-xs font-bold mt-2 ${step >= 1 ? 'text-blue-600' : 'text-gray-500'}`}>PERSONAL DETAILS</span>
          </div>
          
          <div className="flex flex-col items-center">
            <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold ${step >= 2 ? 'bg-blue-600 text-white shadow-md' : 'bg-white border-2 border-gray-300 text-gray-400'}`}>📋</div>
            <span className={`text-xs font-bold mt-2 ${step >= 2 ? 'text-blue-600' : 'text-gray-500'}`}>MEDICAL HISTORY</span>
          </div>
          
          <div className="flex flex-col items-center">
            <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold ${step >= 3 ? 'bg-blue-600 text-white shadow-md' : 'bg-white border-2 border-gray-300 text-gray-400'}`}>📞</div>
            <span className={`text-xs font-bold mt-2 ${step >= 3 ? 'text-blue-600' : 'text-gray-500'}`}>CONTACT & EMERGENCY</span>
          </div>
        </div>

        {error && <div className="mb-6 p-4 bg-red-100 text-red-700 rounded-lg">{error}</div>}

        {/* Form Card */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden mb-12">
          <div className="bg-slate-50/50 p-6 border-b border-gray-100 flex items-center space-x-4">
            <div className="bg-blue-100 text-blue-600 p-3 rounded-lg">
              {step === 1 ? '👤' : step === 2 ? '📋' : '📞'}
            </div>
            <div>
              <h2 className="text-xl font-bold text-gray-900">
                {step === 1 ? 'Personal Information' : step === 2 ? 'Medical History' : 'Contact Details'}
              </h2>
              <p className="text-sm text-gray-500">
                {step === 1 ? 'Collect basic identity and access details.' : step === 2 ? 'Basic medical and demographic profile.' : 'Where can we reach you or your family.'}
              </p>
            </div>
          </div>

          <form onSubmit={step === 3 ? handleRegister : (e) => e.preventDefault()} className="p-8">
            
            {/* STEP 1: Personal Details */}
            {step === 1 && (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-1">Full Name</label>
                  <input type="text" name="fullName" value={formData.fullName} onChange={handleChange} required className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Email Address</label>
                  <input type="email" name="email" value={formData.email} onChange={handleChange} required className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Mobile Number</label>
                  <input type="text" name="phone" value={formData.phone} onChange={handleChange} required className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
                  <input type="password" name="password" value={formData.password} onChange={handleChange} required className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Confirm Password</label>
                  <input type="password" name="confirmPassword" value={formData.confirmPassword} onChange={handleChange} required className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none" />
                </div>
              </div>
            )}

            {/* STEP 2: Medical History */}
            {step === 2 && (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Date of Birth</label>
                  <input type="date" name="dob" value={formData.dob} onChange={handleChange} required className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Gender</label>
                  <select name="gender" value={formData.gender} onChange={handleChange} required className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none bg-white">
                    <option value="">Select gender</option>
                    <option value="Male">Male</option>
                    <option value="Female">Female</option>
                    <option value="Other">Other</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Primary Pain Type</label>
                  <select name="painType" value={formData.painType} onChange={handleChange} required className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none bg-white">
                    <option value="">Select pain type</option>
                    <option value="Aching">Aching</option>
                    <option value="Burning">Burning</option>
                    <option value="Sharp">Sharp</option>
                    <option value="Throbbing">Throbbing</option>
                    <option value="Stiffness">Stiffness</option>
                    <option value="Other">Other</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Pain Rating (1-10)</label>
                  <input type="number" name="painRating" min="1" max="10" value={formData.painRating} onChange={handleChange} placeholder="e.g. 5" required className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none" />
                </div>
              </div>
            )}

            {/* STEP 3: Contact & Emergency */}
            {step === 3 && (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-1">Full Address</label>
                  <textarea name="address" value={formData.address} onChange={handleChange} rows="3" required className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"></textarea>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Emergency Contact Name</label>
                  <input type="text" name="emergencyContactName" value={formData.emergencyContactName} onChange={handleChange} required className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Emergency Contact Number</label>
                  <input type="text" name="emergencyContactNumber" value={formData.emergencyContactNumber} onChange={handleChange} required className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none" />
                </div>
                <div className="md:col-span-2 flex items-center mt-2">
                  <input type="checkbox" name="termsAccepted" checked={formData.termsAccepted} onChange={handleChange} className="h-4 w-4 text-blue-600 border-gray-300 rounded" />
                  <label className="ml-2 block text-sm text-gray-700">I agree to the Terms & Conditions and Privacy Policy</label>
                </div>
              </div>
            )}

            {/* Navigation Buttons */}
            <div className="flex justify-between items-center border-t border-gray-100 pt-6">
              {step === 1 ? (
                <Link to="/login" className="text-gray-500 hover:text-gray-700 font-medium">Cancel</Link>
              ) : (
                <button type="button" onClick={handleBack} className="text-gray-500 hover:text-gray-700 font-medium px-4 py-2">&larr; Back</button>
              )}
              
              {step < 3 ? (
                <button type="button" onClick={handleNext} className="bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 px-8 rounded-lg transition-colors">
                  Next Step &rarr;
                </button>
              ) : (
                <button type="submit" className="bg-green-600 hover:bg-green-700 text-white font-medium py-3 px-8 rounded-lg transition-colors">
                  Complete Registration
                </button>
              )}
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}